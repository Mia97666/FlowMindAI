package com.flowmind.workflow.engine.handler;

import com.flowmind.notification.service.NotificationService;
import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.entity.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 通知节点处理器。
 *
 * 节点配置示例：
 * {
 *   "receivers": "starter,finance",
 *   "title": "审批结束",
 *   "content": "流程 {title} 已处理"
 * }
 */
@Component
@RequiredArgsConstructor
public class NotifyNodeHandler implements NodeHandler {

    private final NotificationService notificationService;

    @Override
    public boolean support(String nodeType) {
        return "NOTIFY".equalsIgnoreCase(nodeType);
    }

    @Override
    public NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    ) {
        WorkflowInstance instance = context.getInstance();
        String receivers = text(node.getConfig().getOrDefault("receivers", "starter"));
        String title = render(text(node.getConfig().getOrDefault("title", "审批流程通知")), instance);
        String content = render(text(node.getConfig().getOrDefault("content", "流程 {title} 已流转到通知节点。")), instance);

        for (String receiver : receivers.split(",")) {
            String realReceiver = normalizeReceiver(receiver.trim(), instance);
            if (!realReceiver.isBlank()) {
                notificationService.create(
                        realReceiver,
                        title,
                        content,
                        "WORKFLOW_NOTIFY",
                        instance.getId()
                );
            }
        }

        return NodeExecutionResult.continueNext();
    }

    private String normalizeReceiver(String receiver, WorkflowInstance instance) {
        if ("starter".equalsIgnoreCase(receiver)) {
            return instance.getStarter();
        }
        return receiver;
    }

    private String render(String template, WorkflowInstance instance) {
        return template
                .replace("{title}", nullToText(instance.getTitle()))
                .replace("{starter}", nullToText(instance.getStarter()))
                .replace("{riskScore}", instance.getRiskScore() == null ? "未评估" : String.valueOf(instance.getRiskScore()))
                .replace("{riskLevel}", nullToText(instance.getRiskLevel()));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullToText(String value) {
        return value == null ? "" : value;
    }
}
