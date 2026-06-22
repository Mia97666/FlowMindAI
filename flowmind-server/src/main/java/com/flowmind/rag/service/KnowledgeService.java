package com.flowmind.rag.service;

import com.flowmind.rag.dto.RagResponse;

/**
 * 知识库服务接口。
 * <p>
 * 用于屏蔽底层知识库实现。
 * 当前实现是自研 RAG，后续可以扩展 RAGFlow、百炼知识库等。
 */
public interface KnowledgeService {

    /**
     * 根据问题查询企业知识库（使用默认检索参数）。
     *
     * @param question 用户问题或审批规则查询
     * @return 知识库回答
     */
    default RagResponse ask(String question) {
        return ask(question, null, null);
    }

    /**
     * 根据问题查询企业知识库，支持指定检索参数。
     *
     * @param question 用户问题
     * @param topK     召回 TopK 数量，为空时使用默认值
     * @param minScore 向量相似度最低阈值，为空时使用默认值
     * @return 知识库回答
     */
    RagResponse ask(String question, Integer topK, Double minScore);
}
