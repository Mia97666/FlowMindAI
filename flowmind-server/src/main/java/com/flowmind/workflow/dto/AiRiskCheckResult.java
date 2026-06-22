package com.flowmind.workflow.dto;

import com.flowmind.rag.dto.RagSource;
import lombok.Data;

import java.util.List;

/**
 * AI 风险检测结果。
 *
 * 由大模型返回结构化 JSON 后解析而来。
 */
@Data
public class AiRiskCheckResult {

    /**
     * 风险评分。
     *
     * 0-30：低风险
     * 31-69：中风险
     * 70-100：高风险
     */
    private Integer riskScore;

    /**
     * 风险等级。
     *
     * LOW / MEDIUM / HIGH
     */
    private String riskLevel;

    /**
     * 风险原因。
     */
    private String riskReason;

    /**
     * 处理建议。
     */
    private String suggestion;

    /**
     * AI 审批决策。
     *
     * APPROVED：低风险自动通过；
     * NEED_HUMAN_REVIEW：需要人工复核；
     * REJECTED：AI 判断不符合制度，建议拒绝。
     */
    private String decision;

    /**
     * RAG 来源引用。
     */
    private List<RagSource> sources;

    /**
     * 实际用于风险检测的大模型 Prompt。
     */
    private String prompt;

    /**
     * RAG 返回的制度上下文摘要。
     */
    private String ragContext;

    /**
     * 大模型原始返回内容。
     */
    private String rawResponse;

    /**
     * 使用的大模型名称。
     */
    private String modelName;

    /**
     * 执行耗时，单位毫秒。
     */
    private Long durationMs;
}
