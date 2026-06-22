package com.flowmind.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 审批审计日志。
 *
 * 每一次 AI 节点执行都会落库，保证风险评分、依据来源和模型原始返回可追溯。
 */
@Data
@Entity
@Table(name = "ai_audit_log")
public class AiAuditLog {

    /**
     * 审计日志主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程实例ID。
     */
    private Long instanceId;

    /**
     * 流程编码。
     */
    private String workflowCode;

    /**
     * AI 节点ID。
     */
    private String nodeId;

    /**
     * 风险分数。
     */
    private Integer riskScore;

    /**
     * 风险等级。
     */
    private String riskLevel;

    /**
     * AI 审批决策。
     */
    private String decision;

    /**
     * 风险原因。
     */
    @Column(columnDefinition = "TEXT")
    private String riskReason;

    /**
     * AI 建议。
     */
    @Column(columnDefinition = "TEXT")
    private String suggestion;

    /**
     * 实际发送给大模型的 Prompt。
     */
    @Column(columnDefinition = "TEXT")
    private String prompt;

    /**
     * RAG 压缩后的制度上下文。
     */
    @Column(columnDefinition = "TEXT")
    private String ragContext;

    /**
     * RAG 引用来源 JSON。
     */
    @Column(columnDefinition = "TEXT")
    private String ragSourcesJson;

    /**
     * 使用的大模型名称。
     */
    private String modelName;

    /**
     * 大模型原始返回。
     */
    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    /**
     * 执行耗时，单位毫秒。
     */
    private Long durationMs;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
