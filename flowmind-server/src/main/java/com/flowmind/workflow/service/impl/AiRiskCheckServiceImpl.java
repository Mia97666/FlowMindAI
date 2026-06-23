package com.flowmind.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.chat.ChatService;
import com.flowmind.rag.adapter.RagKnowledgeAdapter;
import com.flowmind.rag.dto.RagResponse;
import com.flowmind.workflow.dto.AiRiskCheckResult;
import com.flowmind.workflow.dto.AiRiskExecutionRequest;
import com.flowmind.workflow.risk.AiRiskStrategyConfig;
import com.flowmind.workflow.risk.AiRiskStrategyResolver;
import com.flowmind.workflow.risk.RiskRuleEvaluation;
import com.flowmind.workflow.risk.RiskRuleMatch;
import com.flowmind.workflow.risk.RiskRuleEngine;
import com.flowmind.workflow.service.AiRiskCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI 风险检测服务实现。
 *
 * 执行顺序：
 * 1. 根据节点配置解析策略
 * 2. 先执行配置化本地规则
 * 3. 必要时再走 RAG + 大模型
 * 4. 统一生成风险等级、决策、审计字段和耗时日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRiskCheckServiceImpl implements AiRiskCheckService {

    private static final String LLM_MODE_NEVER = "NEVER";
    private static final String LLM_MODE_ALWAYS = "ALWAYS";

    private final RagKnowledgeAdapter ragKnowledgeAdapter;

    private final ChatService chatService;

    private final ObjectMapper objectMapper;

    private final AiRiskStrategyResolver strategyResolver;

    private final RiskRuleEngine riskRuleEngine;

    @Value("${dashscope.model:qwen-plus}")
    private String modelName;

    @Override
    public AiRiskCheckResult check(AiRiskExecutionRequest request) {
        Instant totalStart = Instant.now();
        AiRiskExecutionRequest safeRequest = normalizeRequest(request);

        Instant strategyStart = Instant.now();
        AiRiskStrategyConfig strategy = strategyResolver.resolve(safeRequest);
        long strategyMs = elapsed(strategyStart);

        Instant ruleStart = Instant.now();
        RiskRuleEvaluation ruleEvaluation = riskRuleEngine.evaluate(strategy, safeRequest.getBusinessData());
        long ruleMs = elapsed(ruleStart);

        boolean callLlm = shouldCallLlm(strategy, ruleEvaluation);
        if (!callLlm) {
            AiRiskCheckResult result = buildRuleResult(strategy, safeRequest, ruleEvaluation, totalStart);
            logRiskTiming(safeRequest, strategy, result, strategyMs, ruleMs, 0, 0, true);
            return result;
        }

        Instant ragStart = Instant.now();
        RagResponse policyResponse = new RagResponse("", List.of());
        long ragMs = 0;
        try {
            if (callLlm) {
                String policyQuestion = buildPolicyQuestion(strategy, safeRequest);
                policyResponse = ragKnowledgeAdapter.ask(
                        policyQuestion,
                        strategy.getTopK(),
                        strategy.getMinScore()
                );
                ragMs = elapsed(ragStart);
            }

            Instant llmStart = Instant.now();
            String prompt = buildPrompt(strategy, safeRequest, ruleEvaluation, policyResponse);
            String json = chatService.chat(prompt).trim();
            long llmMs = elapsed(llmStart);

            AiRiskCheckResult result = parseModelResult(json, strategy);
            applyRuleFloor(strategy, ruleEvaluation, result);
            fillAuditFields(result, prompt, policyResponse, json, totalStart);
            logRiskTiming(safeRequest, strategy, result, strategyMs, ruleMs, ragMs, llmMs, false);
            return result;
        } catch (Exception e) {
            AiRiskCheckResult result = buildSafeFallback(strategy, safeRequest, ruleEvaluation, e, totalStart);
            logRiskTiming(safeRequest, strategy, result, strategyMs, ruleMs, ragMs, 0, ruleEvaluation.hasMatches());
            return result;
        }
    }

    private AiRiskExecutionRequest normalizeRequest(AiRiskExecutionRequest request) {
        AiRiskExecutionRequest safeRequest = request == null ? new AiRiskExecutionRequest() : request;
        if (safeRequest.getNodeConfig() == null) {
            safeRequest.setNodeConfig(Map.of());
        }
        if (safeRequest.getBusinessData() == null) {
            safeRequest.setBusinessData(Map.of());
        }
        return safeRequest;
    }

    private boolean shouldCallLlm(AiRiskStrategyConfig strategy, RiskRuleEvaluation ruleEvaluation) {
        String llmMode = strategy.getLlmMode() == null ? "" : strategy.getLlmMode().trim().toUpperCase();
        if (LLM_MODE_NEVER.equals(llmMode)) {
            return false;
        }
        if (LLM_MODE_ALWAYS.equals(llmMode)) {
            return true;
        }
        return !ruleEvaluation.hasMatches();
    }

    private AiRiskCheckResult buildRuleResult(
            AiRiskStrategyConfig strategy,
            AiRiskExecutionRequest request,
            RiskRuleEvaluation ruleEvaluation,
            Instant start
    ) {
        AiRiskCheckResult result = new AiRiskCheckResult();
        result.setRiskScore(ruleEvaluation.getRiskScore());
        fillRiskLevel(result, strategy);
        result.setRiskReason(buildRuleReason(strategy, request, ruleEvaluation));
        result.setSuggestion(firstNotBlank(ruleEvaluation.getSuggestion(), strategy.getDefaultSuggestion()));
        result.setDecision(resolveDecision(result, strategy));
        result.setPrompt("LOCAL_RULE_ENGINE:" + strategy.getStrategyCode());
        result.setRagContext("未调用 RAG/LLM，本次由配置化本地规则直接生成风险结果。");
        result.setSources(List.of());
        result.setRawResponse(toJson(ruleEvaluation.getMatches()));
        result.setModelName("local-risk-rule-engine");
        result.setDurationMs(elapsed(start));
        return result;
    }

    private AiRiskCheckResult buildSafeFallback(
            AiRiskStrategyConfig strategy,
            AiRiskExecutionRequest request,
            RiskRuleEvaluation ruleEvaluation,
            Exception exception,
            Instant start
    ) {
        if (ruleEvaluation.hasMatches()) {
            AiRiskCheckResult result = buildRuleResult(strategy, request, ruleEvaluation, start);
            result.setRiskReason(result.getRiskReason() + "；RAG/LLM 不可用，已保留本地规则结果。异常：" + exception.getMessage());
            result.setRawResponse(exception.getMessage());
            return result;
        }

        AiRiskCheckResult result = new AiRiskCheckResult();
        result.setRiskScore(55);
        fillRiskLevel(result, strategy);
        result.setRiskReason("RAG/LLM 不可用且未命中本地策略规则，系统保守转人工复核。流程："
                + text(request.getWorkflowCode()) + "；节点：" + text(request.getNodeName())
                + "；异常：" + exception.getMessage());
        result.setSuggestion("建议人工复核该申请，并检查策略规则、知识库和大模型服务状态。");
        result.setDecision("NEED_HUMAN_REVIEW");
        result.setPrompt("SAFE_FALLBACK:" + strategy.getStrategyCode());
        result.setRagContext("未取得有效制度上下文。");
        result.setSources(List.of());
        result.setRawResponse(exception.getMessage());
        result.setModelName("safe-fallback");
        result.setDurationMs(elapsed(start));
        return result;
    }

    private void applyRuleFloor(
            AiRiskStrategyConfig strategy,
            RiskRuleEvaluation ruleEvaluation,
            AiRiskCheckResult result
    ) {
        if (!ruleEvaluation.hasMatches()) {
            fillRiskLevel(result, strategy);
            result.setDecision(resolveDecision(result, strategy));
            return;
        }
        if (result.getRiskScore() == null || result.getRiskScore() < ruleEvaluation.getRiskScore()) {
            result.setRiskScore(ruleEvaluation.getRiskScore());
        }
        fillRiskLevel(result, strategy);
        String ruleReason = buildRuleReason(strategy, null, ruleEvaluation);
        result.setRiskReason(ruleReason + "；模型原始判断：" + text(result.getRiskReason()));
        result.setSuggestion(firstNotBlank(ruleEvaluation.getSuggestion(), result.getSuggestion(), strategy.getDefaultSuggestion()));
        result.setDecision(resolveDecision(result, strategy));
    }

    private String buildPolicyQuestion(AiRiskStrategyConfig strategy, AiRiskExecutionRequest request) {
        return """
                流程编码：%s
                节点：%s
                风险策略：%s（%s）
                RAG 检索范围：%s
                业务数据：%s

                请检索该申请涉及的审批规则、金额阈值、授权要求、用印要求、风控要求和人工复核条件。
                """.formatted(
                text(request.getWorkflowCode()),
                firstNotBlank(request.getNodeName(), request.getNodeId()),
                text(strategy.getStrategyName()),
                text(strategy.getStrategyCode()),
                text(strategy.getRagScope()),
                request.getBusinessData()
        );
    }

    private String buildPrompt(
            AiRiskStrategyConfig strategy,
            AiRiskExecutionRequest request,
            RiskRuleEvaluation ruleEvaluation,
            RagResponse policyResponse
    ) {
        String customPrompt = firstNotBlank(
                strategy.getPromptTemplate(),
                "请结合业务数据、制度依据和本地规则命中结果，输出风险评分、风险等级、风险原因和处理建议。"
        );
        return """
                你是 FlowMind AI 企业审批风险检测助手。

                策略：%s（%s）
                策略要求：%s

                风险评分规则：
                - 0 到 %d：低风险
                - %d 到 %d：中风险
                - %d 到 100：高风险

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

                【节点】
                %s

                【业务数据】
                %s

                【本地规则命中】
                %s

                【企业制度依据】
                %s
                """.formatted(
                text(strategy.getStrategyName()),
                text(strategy.getStrategyCode()),
                customPrompt,
                strategy.getLowMaxScore(),
                strategy.getLowMaxScore() + 1,
                strategy.getHighMinScore() - 1,
                strategy.getHighMinScore(),
                text(request.getWorkflowCode()),
                firstNotBlank(request.getNodeName(), request.getNodeId()),
                request.getBusinessData(),
                toJson(ruleEvaluation.getMatches()),
                policyResponse == null ? "" : text(policyResponse.getAnswer())
        );
    }

    private AiRiskCheckResult parseModelResult(String json, AiRiskStrategyConfig strategy) throws Exception {
        String cleanedJson = stripMarkdownFence(json);
        AiRiskCheckResult result = objectMapper.readValue(
                cleanedJson,
                new TypeReference<>() {}
        );
        fillRiskLevel(result, strategy);
        result.setDecision(resolveDecision(result, strategy));
        return result;
    }

    private String stripMarkdownFence(String json) {
        String value = json == null ? "" : json.trim();
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
        result.setRagContext(policyResponse == null ? "" : policyResponse.getAnswer());
        result.setSources(policyResponse == null || policyResponse.getSources() == null
                ? List.of()
                : policyResponse.getSources());
        result.setRawResponse(rawResponse);
        result.setModelName(modelName);
        result.setDurationMs(elapsed(start));
    }

    private void fillRiskLevel(AiRiskCheckResult result, AiRiskStrategyConfig strategy) {
        if (result.getRiskLevel() != null || result.getRiskScore() == null) {
            return;
        }
        if (result.getRiskScore() >= strategy.getHighMinScore()) {
            result.setRiskLevel("HIGH");
        } else if (result.getRiskScore() > strategy.getLowMaxScore()) {
            result.setRiskLevel("MEDIUM");
        } else {
            result.setRiskLevel("LOW");
        }
    }

    private String resolveDecision(AiRiskCheckResult result, AiRiskStrategyConfig strategy) {
        if (result == null || result.getRiskScore() == null) {
            return "NEED_HUMAN_REVIEW";
        }
        return "LOW".equalsIgnoreCase(result.getRiskLevel())
                || result.getRiskScore() <= strategy.getAutoApproveMaxScore()
                ? "APPROVED"
                : "NEED_HUMAN_REVIEW";
    }

    private String buildRuleReason(
            AiRiskStrategyConfig strategy,
            AiRiskExecutionRequest request,
            RiskRuleEvaluation ruleEvaluation
    ) {
        String matchedReasons = ruleEvaluation.getMatches().stream()
                .map(this::formatMatch)
                .toList()
                .toString();
        String workflowText = request == null ? "" : "；流程：" + text(request.getWorkflowCode());
        return "系统按策略【" + strategy.getStrategyName() + "】执行本地配置规则。命中规则："
                + matchedReasons
                + workflowText;
    }

    private String formatMatch(RiskRuleMatch match) {
        return match.getReason() + "（" + match.getRuleId() + "，+" + match.getScore() + "）";
    }

    private void logRiskTiming(
            AiRiskExecutionRequest request,
            AiRiskStrategyConfig strategy,
            AiRiskCheckResult result,
            long strategyMs,
            long ruleMs,
            long ragMs,
            long llmMs,
            boolean localOnly
    ) {
        log.info(
                "AI risk check finished workflowCode={}, nodeId={}, strategyCode={}, riskScore={}, riskLevel={}, decision={}, localOnly={}, strategyMs={}, ruleMs={}, ragMs={}, llmMs={}, totalMs={}",
                request.getWorkflowCode(),
                request.getNodeId(),
                strategy.getStrategyCode(),
                result.getRiskScore(),
                result.getRiskLevel(),
                result.getDecision(),
                localOnly,
                strategyMs,
                ruleMs,
                ragMs,
                llmMs,
                result.getDurationMs()
        );
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private long elapsed(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }
}
