package com.flowmind.workflow.engine.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.audit.service.AiAuditService;
import com.flowmind.notification.service.NotificationService;
import com.flowmind.workflow.dto.AiRiskCheckResult;
import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.service.WorkflowActionLogService;
import com.flowmind.workflow.service.AiRiskCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 风险检测/AI 审批节点处理器。
 *
 * 该节点会调用 RAG + 大模型生成风险评分、制度依据和审批建议，
 * 并把结果写回流程实例与 AI 审计日志。
 */
@Component
@RequiredArgsConstructor
public class AiRiskNodeHandler implements NodeHandler {

    private static final int DEFAULT_RISK_THRESHOLD = 70;

    private static final int DEFAULT_AUTO_APPROVE_MAX_SCORE = 30;

    private final AiRiskCheckService aiRiskCheckService;

    private final AiAuditService aiAuditService;

    private final NotificationService notificationService;

    private final WorkflowActionLogService actionLogService;

    private final ObjectMapper objectMapper;

    @Override
    public boolean support(String nodeType) {
        return "AI_RISK_CHECK".equalsIgnoreCase(nodeType)
                || "AI_APPROVAL".equalsIgnoreCase(nodeType);
    }

    @Override
    public NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    ) {
        WorkflowInstance instance = context.getInstance();

        AiRiskCheckResult result = aiRiskCheckService.check(
                instance.getDefinitionCode(),
                context.getBusinessData()
        );
        applyThresholdDecision(result, node);
        fillInstanceRiskResult(instance, result);
        aiAuditService.record(instance.getId(), instance.getDefinitionCode(), node.getId(), result);
        actionLogService.recordInstanceAction(
                instance,
                "AI_RISK_CHECK",
                "SYSTEM",
                "AI 风险评分：" + result.getRiskScore()
                        + "，等级：" + text(result.getRiskLevel())
                        + "，决策：" + text(result.getDecision())
                        + "，依据：" + text(result.getRiskReason())
        );
        recordAiDecision(instance, node, result);
        sendHighRiskNotification(instance, node, result);

        return NodeExecutionResult.continueNext();
    }

    /**
     * 节点可以配置自己的风险阈值，未配置则使用系统默认 70。
     */
    private void applyThresholdDecision(
            AiRiskCheckResult result,
            WorkflowNode node
    ) {
        int autoApproveMaxScore = intConfig(
                node,
                "autoApproveMaxScore",
                DEFAULT_AUTO_APPROVE_MAX_SCORE
        );
        if (result.getRiskScore() == null) {
            result.setDecision("NEED_HUMAN_REVIEW");
            return;
        }

        boolean lowRisk = "LOW".equalsIgnoreCase(text(result.getRiskLevel()))
                || result.getRiskScore() <= autoApproveMaxScore;
        result.setDecision(lowRisk ? "APPROVED" : "NEED_HUMAN_REVIEW");
    }

    /**
     * 将 AI 结果写回实例，方便列表、详情和审批待办直接展示。
     */
    private void fillInstanceRiskResult(
            WorkflowInstance instance,
            AiRiskCheckResult result
    ) {
        instance.setRiskScore(result.getRiskScore());
        instance.setRiskLevel(result.getRiskLevel());
        instance.setRiskReason(result.getRiskReason());
        instance.setAiSuggestion(result.getSuggestion());
        instance.setRagSourcesJson(toJson(result.getSources()));
    }

    /**
     * 高风险申请主动给财务和管理员发站内通知。
     */
    private void sendHighRiskNotification(
            WorkflowInstance instance,
            WorkflowNode node,
            AiRiskCheckResult result
    ) {
        int threshold = intConfig(node, "threshold", DEFAULT_RISK_THRESHOLD);
        if (result.getRiskScore() == null || result.getRiskScore() < threshold) {
            return;
        }

        String receivers = text(node.getConfig().getOrDefault("highRiskReceivers", "finance,admin"));
        for (String receiver : receivers.split(",")) {
            String trimmedReceiver = receiver.trim();
            if (!trimmedReceiver.isBlank()) {
                notificationService.create(
                        trimmedReceiver,
                        "高风险审批提醒：" + instance.getTitle(),
                        "AI 风险评分 " + result.getRiskScore() + "，建议：" + result.getSuggestion(),
                        "HIGH_RISK",
                        instance.getId()
                );
            }
        }
    }

    /**
     * AI 自动审批不是“无痕通过”，必须写入独立动作日志。
     * 后续实例追踪页面会基于该日志解释为什么没有创建人工待办。
     */
    private void recordAiDecision(
            WorkflowInstance instance,
            WorkflowNode node,
            AiRiskCheckResult result
    ) {
        if ("APPROVED".equalsIgnoreCase(text(result.getDecision()))) {
            actionLogService.recordInstanceAction(
                    instance,
                    "AI_AUTO_APPROVED",
                    text(node.getConfig().getOrDefault("autoApproveActor", "ai_approver")),
                    "低风险自动审批通过，评分：" + result.getRiskScore()
                            + "，等级：" + text(result.getRiskLevel())
                            + "，建议：" + text(result.getSuggestion())
            );
            return;
        }

        actionLogService.recordInstanceAction(
                instance,
                "AI_HUMAN_REVIEW_REQUIRED",
                "SYSTEM",
                "AI 判断需要人工复核，评分：" + result.getRiskScore()
                        + "，等级：" + text(result.getRiskLevel())
                        + "，建议：" + text(result.getSuggestion())
        );
    }

    private int intConfig(
            WorkflowNode node,
            String key,
            int defaultValue
    ) {
        Object value = node.getConfig().get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
