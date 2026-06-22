package com.flowmind.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.notification.service.NotificationService;
import com.flowmind.workflow.engine.handler.NodeHandler;
import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.NodeExecutionStatus;
import com.flowmind.workflow.engine.model.WorkflowEdge;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowGraph;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.engine.support.ConditionEvaluator;
import com.flowmind.workflow.engine.support.WorkflowGraphParser;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;
import com.flowmind.workflow.entity.WorkflowVersion;
import com.flowmind.workflow.repository.WorkflowDefinitionRepository;
import com.flowmind.workflow.repository.WorkflowInstanceRepository;
import com.flowmind.workflow.repository.WorkflowVersionRepository;
import com.flowmind.workflow.service.WorkflowActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态工作流执行服务实现。
 *
 * 设计思路：
 * 1. definitionJson 只负责描述流程图；
 * 2. NodeHandler 只负责执行节点自身动作；
 * 3. 本服务统一负责节点流转、状态保存和异常边界。
 */
@Service
@RequiredArgsConstructor
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    /**
     * 防止错误流程图出现环路后无限执行。
     */
    private static final int MAX_EXECUTION_STEPS = 100;

    private final WorkflowDefinitionRepository definitionRepository;

    private final WorkflowVersionRepository versionRepository;

    private final WorkflowInstanceRepository instanceRepository;

    private final WorkflowGraphParser graphParser;

    private final ConditionEvaluator conditionEvaluator;

    private final List<NodeHandler> nodeHandlers;

    private final NotificationService notificationService;

    private final WorkflowActionLogService actionLogService;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public WorkflowInstance start(WorkflowInstance instance) {
        WorkflowDefinition definition = getExecutableDefinition(instance);
        WorkflowGraph graph = graphParser.parse(definition.getDefinitionJson());
        WorkflowNode startNode = graphParser.findStartNode(graph);
        if (startNode == null) {
            finishInstance(instance, "COMPLETED", "流程没有配置节点，系统自动结束。");
            return instanceRepository.save(instance);
        }
        return execute(instance, definition, graph, startNode.getId());
    }

    @Override
    @Transactional
    public WorkflowInstance continueAfterTask(WorkflowTask task) {
        WorkflowInstance instance = getInstance(task.getInstanceId());
        WorkflowDefinition definition = getExecutableDefinition(instance);
        WorkflowGraph graph = graphParser.parse(definition.getDefinitionJson());
        String nextNodeId = chooseNextNode(
                graph,
                task.getNodeId(),
                instance,
                readBusinessData(instance)
        );

        if (nextNodeId == null) {
            finishInstance(instance, "COMPLETED", "审批节点后没有后续节点，流程自动完成。");
            return instanceRepository.save(instance);
        }

        return execute(instance, definition, graph, nextNodeId);
    }

    @Override
    @Transactional
    public WorkflowInstance rejectByTask(WorkflowTask task) {
        WorkflowInstance instance = getInstance(task.getInstanceId());
        finishInstance(instance, "REJECTED", "审批人拒绝：" + nullToText(task.getComment()));
        notificationService.create(
                instance.getStarter(),
                "审批被拒绝：" + instance.getTitle(),
                "节点【" + task.getNodeName() + "】拒绝了该申请，意见：" + nullToText(task.getComment()),
                "WORKFLOW_REJECTED",
                instance.getId()
        );
        actionLogService.recordTaskAction(
                task,
                instance,
                "REJECTED_END",
                task.getAssignee(),
                task.getComment()
        );
        return instanceRepository.save(instance);
    }

    /**
     * 从指定节点开始执行流程。
     */
    private WorkflowInstance execute(
            WorkflowInstance instance,
            WorkflowDefinition definition,
            WorkflowGraph graph,
            String startNodeId
    ) {
        WorkflowExecutionContext context = new WorkflowExecutionContext();
        context.setDefinition(definition);
        context.setInstance(instance);
        context.setGraph(graph);
        context.setBusinessData(readBusinessData(instance));

        String currentNodeId = startNodeId;
        int steps = 0;

        while ("RUNNING".equals(instance.getStatus()) && currentNodeId != null) {
            if (++steps > MAX_EXECUTION_STEPS) {
                throw new IllegalStateException("流程执行超过最大步数，请检查是否存在错误环路。");
            }

            WorkflowNode currentNode = graphParser.findNode(graph, currentNodeId);
            if (currentNode == null) {
                finishInstance(instance, "COMPLETED", "找不到后续节点，流程自动完成。");
                break;
            }

            markCurrentNode(instance, currentNode);
            instance.setUpdatedAt(LocalDateTime.now());
            actionLogService.recordInstanceAction(
                    instance,
                    "ENTER_NODE",
                    "SYSTEM",
                    "进入节点：" + nullToText(currentNode.getName())
            );
            NodeExecutionResult result = findHandler(currentNode).handle(context, currentNode);

            if (NodeExecutionStatus.WAITING.equals(result.getStatus())) {
                return instanceRepository.save(instance);
            }

            if (NodeExecutionStatus.COMPLETED.equals(result.getStatus())) {
                finishInstance(instance, "COMPLETED", "流程正常结束。");
                return instanceRepository.save(instance);
            }

            if (NodeExecutionStatus.REJECTED.equals(result.getStatus())) {
                finishInstance(instance, "REJECTED", "流程节点拒绝。");
                return instanceRepository.save(instance);
            }

            currentNodeId = result.getNextNodeId() == null
                    ? chooseNextNode(graph, currentNode.getId(), instance, context.getBusinessData())
                    : result.getNextNodeId();
        }

        if ("RUNNING".equals(instance.getStatus()) && currentNodeId == null) {
            finishInstance(instance, "COMPLETED", "没有后续节点，流程自动完成。");
        }

        return instanceRepository.save(instance);
    }

    private NodeHandler findHandler(WorkflowNode node) {
        return nodeHandlers.stream()
                .filter(handler -> handler.support(node.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的节点类型：" + node.getType()));
    }

    /**
     * 选择普通节点的下一条边。
     *
     * 先计算显式条件，再使用 default/else/空条件作为兜底分支。
     */
    private String chooseNextNode(
            WorkflowGraph graph,
            String sourceNodeId,
            WorkflowInstance instance,
            Map<String, Object> businessData
    ) {
        List<WorkflowEdge> outgoingEdges = graph.getEdges()
                .stream()
                .filter(edge -> sourceNodeId.equals(edge.getSource()))
                .toList();

        WorkflowEdge defaultEdge = null;
        for (WorkflowEdge edge : outgoingEdges) {
            if (conditionEvaluator.isDefaultCondition(edge.getCondition())) {
                defaultEdge = edge;
                continue;
            }
            if (conditionEvaluator.evaluate(edge.getCondition(), instance, businessData)) {
                return edge.getTarget();
            }
        }
        return defaultEdge == null ? null : defaultEdge.getTarget();
    }

    private void markCurrentNode(
            WorkflowInstance instance,
            WorkflowNode node
    ) {
        instance.setCurrentNodeId(node.getId());
        instance.setCurrentNodeName(node.getName());
        instance.setUpdatedAt(LocalDateTime.now());
    }

    private void finishInstance(
            WorkflowInstance instance,
            String status,
            String reason
    ) {
        instance.setStatus(status);
        instance.setCurrentNodeName(reason);
        instance.setUpdatedAt(LocalDateTime.now());
        instance.setCompletedAt(LocalDateTime.now());
        notificationService.create(
                instance.getStarter(),
                "审批流程" + ("COMPLETED".equals(status) ? "已完成：" : "已结束：") + instance.getTitle(),
                reason,
                "WORKFLOW_DONE",
                instance.getId()
        );
        actionLogService.recordInstanceAction(
                instance,
                status,
                "SYSTEM",
                reason
        );
    }

    private WorkflowDefinition getDefinition(Long definitionId) {
        return definitionRepository.findById(definitionId)
                .orElseThrow(() -> new NotFoundException("流程定义", definitionId));
    }

    private WorkflowDefinition getExecutableDefinition(WorkflowInstance instance) {
        WorkflowDefinition definition = getDefinition(instance.getDefinitionId());
        if (instance.getDefinitionVersionId() != null) {
            return versionRepository.findById(instance.getDefinitionVersionId())
                    .map(version -> snapshotDefinition(definition, version))
                    .orElse(definition);
        }
        if (definition.getPublishedVersionId() != null) {
            return versionRepository.findById(definition.getPublishedVersionId())
                    .map(version -> snapshotDefinition(definition, version))
                    .orElse(definition);
        }
        return versionRepository.findTopByDefinitionIdOrderByVersionDesc(definition.getId())
                .map(version -> snapshotDefinition(definition, version))
                .orElse(definition);
    }

    private WorkflowDefinition snapshotDefinition(
            WorkflowDefinition definition,
            WorkflowVersion version
    ) {
        WorkflowDefinition snapshot = new WorkflowDefinition();
        snapshot.setId(definition.getId());
        snapshot.setCode(version.getCode());
        snapshot.setName(version.getName());
        snapshot.setDescription(version.getDescription());
        snapshot.setVersion(version.getVersion());
        snapshot.setPublishedVersionId(version.getId());
        snapshot.setFormJson(version.getFormJson());
        snapshot.setDefinitionJson(version.getDefinitionJson());
        snapshot.setBpmnXml(version.getBpmnXml());
        snapshot.setStatus(version.getStatus());
        snapshot.setEnabled(true);
        snapshot.setPublishedAt(version.getPublishedAt());
        return snapshot;
    }

    private WorkflowInstance getInstance(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("流程实例", instanceId));
    }

    private Map<String, Object> readBusinessData(WorkflowInstance instance) {
        if (instance.getBusinessDataJson() == null || instance.getBusinessDataJson().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> data = objectMapper.readValue(
                    instance.getBusinessDataJson(),
                    new TypeReference<>() {}
            );
            return data == null ? new LinkedHashMap<>() : data;
        } catch (Exception e) {
            throw new IllegalArgumentException("业务数据JSON解析失败：" + e.getMessage(), e);
        }
    }

    private String nullToText(String value) {
        return value == null ? "" : value;
    }
}
