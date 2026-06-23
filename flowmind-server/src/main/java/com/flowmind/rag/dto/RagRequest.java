package com.flowmind.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RAG 问答请求。
 *
 * 前端可指定检索参数和知识库适配器类型，
 * 后端根据 adapterType 路由到对应实现。
 */
@Data
public class RagRequest {

    /**
     * 用户问题。
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 召回 TopK 数量。
     *
     * 为空时由后端按适配器默认值处理。
     */
    private Integer topK;

    /**
     * 向量相似度最低阈值。
     *
     * 为空时由后端按适配器默认值处理。
     */
    private Double minScore;

    /**
     * 知识库适配器类型。
     *
     * SELF：自研 RAG Pipeline
     * RAGFLOW：RAGFlow 外部知识库
     * 为空时默认 SELF。
     */
    private String adapterType;

    /**
     * 检索模式。
     *
     * LIGHT：轻量模式，默认模式，关闭查询重写和多查询扩展。
     * HIGH_RECALL：高召回率模式，允许用户手动开启增强能力。
     */
    private String retrievalMode;

    /**
     * 查询重写开关。
     *
     * 仅在 retrievalMode=HIGH_RECALL 时生效。
     */
    private Boolean queryRewriteEnabled;

    /**
     * 多查询扩展开关。
     *
     * 仅在 retrievalMode=HIGH_RECALL 时生效。
     */
    private Boolean multiQueryEnabled;

    public RagPipelineOptions toPipelineOptions() {
        return new RagPipelineOptions(
                retrievalMode,
                queryRewriteEnabled,
                multiQueryEnabled
        );
    }
}
