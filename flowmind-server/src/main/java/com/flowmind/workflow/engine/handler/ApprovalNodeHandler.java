package com.flowmind.workflow.engine.handler;

import com.flowmind.notification.service.NotificationService;
import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.engine.support.AssigneeResolver;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;
import com.flowmind.workflow.repository.WorkflowTaskRepository;
import com.flowmind.workflow.service.WorkflowActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 人工审批节点处理器。
 *
 * 流程运行到 APPROVAL 节点时创建待办任务，并暂停流程等待审批人处理。
 */
@Component
@RequiredArgsConstructor
public class ApprovalNodeHandler implements NodeHandler {

    private final WorkflowTaskRepository taskRepository;

    private final AssigneeResolver assigneeResolver;

    private final NotificationService notificationService;

    private final WorkflowActionLogService actionLogService;

    @Override
    public boolean support(String nodeType) {
        return "APPROVAL".equalsIgnoreCase(nodeType);
    }

    @Override
    public NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    ) {
        WorkflowInstance instance = context.getInstance();

        if (taskRepository.findByInstanceIdAndNodeIdAndStatus(
                instance.getId(),
                node.getId(),
                "PENDING"
        ).isPresent()) {
            return NodeExecutionResult.waiting();
        }

        String assignee = assigneeResolver.resolve(
                node,
                instance.getStarter(),
                context.getBusinessData()
        );

        WorkflowTask task = new WorkflowTask();
        task.setInstanceId(instance.getId());
        task.setDefinitionId(instance.getDefinitionId());
        task.setDefinitionVersionId(instance.getDefinitionVersionId());
        task.setNodeId(node.getId());
        task.setNodeName(node.getName());
        task.setBindFormVersionId(resolveFormVersionId(node));
        task.setAssignee(assignee);
        task.setStatus("PENDING");
        task.setRiskScore(instance.getRiskScore());
        task.setRiskLevel(instance.getRiskLevel());
        task.setCreatedAt(LocalDateTime.now());
        task = taskRepository.save(task);

        notificationService.create(
                assignee,
                "新的审批待办：" + instance.getTitle(),
                "请处理【" + node.getName() + "】，当前 AI 风险等级：" + nullToText(instance.getRiskLevel()),
                "WORKFLOW_TASK",
                task.getId()
        );

        actionLogService.recordTaskAction(
                task,
                instance,
                "CREATE_TASK",
                "SYSTEM",
                "创建待办任务，处理人：" + assignee
        );

        return NodeExecutionResult.waiting();
    }

    private Long resolveFormVersionId(WorkflowNode node) {
        if (node.getConfig() == null) {
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

    private String nullToText(String value) {
        return value == null ? "未评估" : value;
    }
}
