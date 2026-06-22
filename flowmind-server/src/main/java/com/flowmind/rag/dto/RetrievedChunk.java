package com.flowmind.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RAG 检索命中的知识片段。
 *
 * 这个对象会贯穿：
 * 1. 向量检索
 * 2. 关键词检索
 * 3. 混合检索
 * 4. Rerank 重排序
 */
@Data
@AllArgsConstructor
public class RetrievedChunk {

    /**
     * 文档ID，对应 knowledge_document.id
     */
    private Long documentId;

    /**
     * 文档名称，例如：企业管理制度手册.md
     */
    private String documentName;

    /**
     * Chunk ID，对应 knowledge_chunk.id
     */
    private Long chunkId;

    /**
     * Chunk 在原文档中的顺序
     */
    private Integer chunkIndex;

    /**
     * 当前分数。
     *
     * 向量检索阶段：相似度分数
     * 关键词检索阶段：人为设置的关键词命中分数
     * Rerank 阶段：Qwen Reranker 返回的相关性分数
     */
    private Double score;

    /**
     * Chunk 原文内容
     */
    private String content;

    /**
     * 召回来源：
     * VECTOR / KEYWORD / HYBRID / RERANK
     */
    private String source;
}