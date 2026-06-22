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
}
