package com.flowmind.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.chat.ChatService;
import com.flowmind.rag.adapter.RagKnowledgeAdapter;
import com.flowmind.rag.dto.RagResponse;
import com.flowmind.workflow.dto.AiRiskCheckResult;
import com.flowmind.workflow.service.AiRiskCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI 风险检测服务实现。
 *
 * 该服务对应动态工作流中的 AI_RISK_CHECK 节点。
 *
 * 核心逻辑：
 * 1. 根据流程类型和业务数据构造知识库查询问题
 * 2. 调用 KnowledgeService 查询企业制度
 * 3. 把业务数据 + 制度依据交给大模型
 * 4. 要求大模型输出结构化 JSON
 * 5. 解析 JSON 得到 riskScore、riskLevel、riskReason、suggestion
 */
@Service
@RequiredArgsConstructor
public class AiRiskCheckServiceImpl implements AiRiskCheckService {

    /**
     * 企业知识库适配器。
     *
     * 当前实现是自研 RAG。
     * 后续可以替换成 RAGFlow、百炼知识库等。
     */
    private final RagKnowledgeAdapter ragKnowledgeAdapter;

    /**
     * 大模型对话服务。
     *
     * 用于让 Qwen 根据制度和业务数据进行风险判断。
     */
    private final ChatService chatService;

    /**
     * Jackson JSON 工具。
     *
     * 用于把大模型返回的 JSON 字符串解析成 Java 对象。
     */
    private final ObjectMapper objectMapper;

    /**
     * 当前大模型名称，写入审计日志。
     */
    @Value("${dashscope.model:qwen-plus}")
    private String modelName;

    @Override
    public AiRiskCheckResult check(
            String workflowCode,
            Map<String, Object> businessData
    ) {
        Instant start = Instant.now();
        Map<String, Object> safeBusinessData = businessData == null ? Map.of() : businessData;

        /**
         * 第一步：
         * 构造用于查询企业制度知识库的问题。
         *
         * 这里不要直接问"大模型怎么审批"，
         * 而是先通过 RAG 找制度依据。
         */
        String policyQuestion = """
                流程类型：%s
                业务数据：%s

                请查询该申请涉及的审批规则、金额阈值、审批人要求和风险控制要求。
                """.formatted(workflowCode, safeBusinessData);

        try {
            RagResponse policyResponse = ragKnowledgeAdapter.ask(policyQuestion);

            /**
             * 第二步：
             * 把业务数据和制度依据交给大模型，
             * 让模型做风险评估。
             *
             * 注意：
             * 大模型不是直接凭空判断，
             * 而是基于 RAG 返回的公司制度进行判断。
             */
            String prompt = """
                    你是 FlowMind AI 企业审批风险检测助手。

                    请根据【业务数据】和【企业制度依据】进行风险检测。

                    风险评分规则：
                    - 0 到 30：低风险，可走普通审批
                    - 31 到 69：中风险，需要负责人关注
                    - 70 到 100：高风险，需要财务、总经理或更高级审批人介入

                    请严格输出 JSON，不要输出 Markdown，不要输出解释文字。

                    JSON 格式如下：
                    {
                      "riskScore": 80,
                      "riskLevel": "HIGH",
                      "riskReason": "说明风险原因",
                      "suggestion": "说明处理建议"
                    }

                    【流程编码】
                    %s

                    【业务数据】
                    %s

                    【企业制度依据】
                    %s
                    """.formatted(
                    workflowCode,
                    safeBusinessData,
                    policyResponse.getAnswer()
            );

            String json = chatService.chat(prompt).trim();
            AiRiskCheckResult result = parseModelResult(json);
            fillAuditFields(result, prompt, policyResponse, json, start);
            return result;
        } catch (Exception e) {
            /**
             * 如果 RAG 或模型不可用，
             * 不要让流程崩掉。
             *
             * 企业审批系统中，AI 解析失败应该保守处理：
             * 默认走本地规则并保留失败原因，保证本地演示和审批流执行不中断。
             */
            return buildLocalFallback(workflowCode, safeBusinessData, e, start);
        }
    }

    private AiRiskCheckResult parseModelResult(String json) throws Exception {
        String cleanedJson = stripMarkdownFence(json);
        AiRiskCheckResult result = objectMapper.readValue(
                cleanedJson,
                new TypeReference<>() {}
        );
        fillRiskLevel(result);
        return result;
    }

    private String stripMarkdownFence(String json) {
        String value = json.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        return value;
    }

    private void fillAuditFields(
            AiRiskCheckResult result,
            String prompt,
            RagResponse policyResponse,
            String rawResponse,
            Instant start
    ) {
        result.setPrompt(prompt);
        result.setRagContext(policyResponse.getAnswer());
        result.setSources(policyResponse.getSources() == null ? List.of() : policyResponse.getSources());
        result.setRawResponse(rawResponse);
        result.setModelName(modelName);
        result.setDurationMs(Duration.between(start, Instant.now()).toMillis());
        result.setDecision(isLowRisk(result) ? "APPROVED" : "NEED_HUMAN_REVIEW");
    }

    /**
     * 本地兜底规则。
     *
     * 当外部 RAG/大模型不可用时，按金额做一个保守风险判断，
     * 保证流程引擎、前端和审计链路仍然可以完整演示。
     */
    private AiRiskCheckResult buildLocalFallback(
            String workflowCode,
            Map<String, Object> businessData,
            Exception exception,
            Instant start
    ) {
        BigDecimal amount = readAmount(businessData);
        AiRiskCheckResult result = new AiRiskCheckResult();

        if (amount.compareTo(BigDecimal.valueOf(200000)) >= 0) {
            result.setRiskScore(85);
            result.setRiskLevel("HIGH");
            result.setSuggestion("建议转财务负责人、总经理或更高级审批人复核。");
        } else if (amount.compareTo(BigDecimal.valueOf(50000)) >= 0) {
            result.setRiskScore(55);
            result.setRiskLevel("MEDIUM");
            result.setSuggestion("建议部门负责人或财务人员关注金额合理性。");
        } else {
            result.setRiskScore(25);
            result.setRiskLevel("LOW");
            result.setSuggestion("风险较低，可按普通审批路径处理。");
        }

        result.setRiskReason("外部RAG或大模型暂不可用，系统按本地金额规则兜底评估。流程："
                + workflowCode + "；异常：" + exception.getMessage());
        result.setDecision(isLowRisk(result) ? "APPROVED" : "NEED_HUMAN_REVIEW");
        result.setPrompt("LOCAL_FALLBACK");
        result.setRagContext("本地兜底规则：金额>=200000为高风险，金额>=50000为中风险，其余为低风险。");
        result.setSources(List.of());
        result.setRawResponse(exception.getMessage());
        result.setModelName("local-rule-fallback");
        result.setDurationMs(Duration.between(start, Instant.now()).toMillis());
        return result;
    }

    private BigDecimal readAmount(Map<String, Object> businessData) {
        Object amount = firstNonNull(
                businessData.get("amount"),
                businessData.get("purchaseAmount"),
                businessData.get("expenseAmount"),
                businessData.get("金额")
        );
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(amount));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void fillRiskLevel(AiRiskCheckResult result) {
        if (result.getRiskLevel() != null || result.getRiskScore() == null) {
            return;
        }
        if (result.getRiskScore() >= 70) {
            result.setRiskLevel("HIGH");
        } else if (result.getRiskScore() >= 31) {
            result.setRiskLevel("MEDIUM");
        } else {
            result.setRiskLevel("LOW");
        }
    }

    /**
     * MVP 审批策略：
     * 低风险可由 AI 自动通过；中风险和高风险统一进入人工复核，避免误放行。
     */
    private boolean isLowRisk(AiRiskCheckResult result) {
        if (result == null || result.getRiskScore() == null) {
            return false;
        }
        return "LOW".equalsIgnoreCase(result.getRiskLevel())
                || result.getRiskScore() <= 30;
    }
}
