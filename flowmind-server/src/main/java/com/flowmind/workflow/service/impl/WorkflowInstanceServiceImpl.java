package com.flowmind.workflow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.common.exception.BadRequestException;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.workflow.engine.model.WorkflowGraph;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.engine.support.WorkflowGraphParser;
import com.flowmind.workflow.dto.StartWorkflowRequest;
import com.flowmind.workflow.engine.WorkflowExecutionService;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowVersion;
import com.flowmind.workflow.repository.WorkflowDefinitionRepository;
import com.flowmind.workflow.repository.WorkflowInstanceRepository;
import com.flowmind.workflow.repository.WorkflowVersionRepository;
import com.flowmind.workflow.service.WorkflowActionLogService;
import com.flowmind.workflow.service.WorkflowInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流实例服务实现。
 * <p>
 * 当前职责：
 * 1. 根据流程定义创建实例
 * 2. 保存用户提交的动态表单数据
 * 3. 调用动态流程执行器推进节点
 */
@Service
@RequiredArgsConstructor
public class WorkflowInstanceServiceImpl implements WorkflowInstanceService {

    private final WorkflowDefinitionRepository definitionRepository;

    private final WorkflowVersionRepository versionRepository;

    private final WorkflowInstanceRepository instanceRepository;

    private final ObjectMapper objectMapper;

    private final WorkflowExecutionService workflowExecutionService;

    private final WorkflowGraphParser graphParser;

    private final WorkflowActionLogService actionLogService;

    /**
     * 启动流程实例。
     * <p>
     * 逻辑：
     * 1. 根据 definitionId 查询流程定义
     * 2. 创建 workflow_instance
     * 3. 保存用户提交的业务数据
     * 4. 调用 WorkflowExecutionService 按 definitionJson 动态执行
     */
    @Override
    public WorkflowInstance start(Long definitionId, StartWorkflowRequest request) {
        WorkflowDefinition definition = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new NotFoundException("流程定义", definitionId));
        if (!Boolean.TRUE.equals(definition.getEnabled())) {
            throw new IllegalStateException("流程未启用，不能发起：" + definition.getName());
        }

        WorkflowInstance instance = new WorkflowInstance();
        WorkflowVersion version = resolveExecutableVersion(definition);
        WorkflowGraph executableGraph = graphParser.parse(version == null
                ? definition.getDefinitionJson()
                : version.getDefinitionJson());
        WorkflowNode startNode = graphParser.findStartNode(executableGraph);

        instance.setDefinitionId(definition.getId());
        instance.setDefinitionCode(definition.getCode());
        instance.setDefinitionName(definition.getName());
        instance.setDefinitionVersion(version == null ? definition.getVersion() : version.getVersion());
        instance.setDefinitionVersionId(version == null ? null : version.getId());
        instance.setStartFormVersionId(resolveFormVersionId(startNode));

        instance.setStarter(request.getStarter());
        instance.setTitle(request.getTitle());

        try {
            instance.setBusinessDataJson(
                    objectMapper.writeValueAsString(request.getBusinessData() == null
                            ? Map.of()
                            : request.getBusinessData())
            );
        } catch (Exception e) {
            throw new BadRequestException("业务数据JSON序列化失败", e);
        }

        instance.setCurrentNodeId("start");
        instance.setCurrentNodeName("准备启动");

        /**
         * 流程刚启动时，状态为运行中。
         */
        instance.setStatus("RUNNING");

        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());

        WorkflowInstance savedInstance = instanceRepository.save(instance);
        actionLogService.recordInstanceAction(
                savedInstance,
                "START",
                savedInstance.getStarter(),
                "发起流程"
        );
        return workflowExecutionService.start(savedInstance);
    }

    @Override
    public List<WorkflowInstance> list(String starter) {
        if (starter != null && !starter.isBlank()) {
            return instanceRepository.findByStarterOrderByCreatedAtDesc(starter);
        }
        return instanceRepository.findAll();
    }

    @Override
    public Page<WorkflowInstance> queryPage(
            String starter,
            String title,
            Long definitionId,
            String status,
            String riskLevel,
            Pageable pageable
    ) {
        Specification<WorkflowInstance> specification = SpecificationBuilder.<WorkflowInstance>builder()
                .equal("starter", starter)
                .like("title", title)
                .equal("definitionId", definitionId)
                .equal("status", status)
                .equal("riskLevel", riskLevel)
                .build();
        return instanceRepository.findAll(specification, pageable);
    }

    @Override
    public WorkflowInstance get(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("流程实例", id));
    }

    private WorkflowVersion resolveExecutableVersion(WorkflowDefinition definition) {
        if (definition.getPublishedVersionId() != null) {
            return versionRepository.findById(definition.getPublishedVersionId()).orElse(null);
        }
        return versionRepository.findTopByDefinitionIdOrderByVersionDesc(definition.getId()).orElse(null);
    }

    private Long resolveFormVersionId(WorkflowNode node) {
        if (node == null || node.getConfig() == null) {
            return null;
        }
        Object formVersionId = node.getConfig().get("formVersionId");
        if (formVersionId == null || String.valueOf(formVersionId).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(formVersionId));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
