package com.flowmind.rag.adapter;

import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.dto.RagPipelineOptions;

/**
 * RAG 知识库适配器。
 *
 * 该接口用于隔离"流程引擎需要知识库能力"和"底层知识库具体实现"。
 * 当前默认走自研 RAG Pipeline，后续接入 RAGFlow 时只需要新增实现并切换注入策略。
 */
public interface RagKnowledgeAdapter {

    /**
     * 根据问题检索制度并生成可解释回答（使用默认检索参数）。
     */
    default RagResponse ask(String question) {
        return ask(question, null, null);
    }

    /**
     * 根据问题检索制度并生成可解释回答，支持指定检索参数。
     *
     * @param question  用户问题
     * @param topK      召回 TopK 数量，为空时由实现按默认值处理
     * @param minScore  向量相似度最低阈值，为空时由实现按默认值处理
     * @return 知识库回答
     */
    default RagResponse ask(String question, Integer topK, Double minScore) {
        return ask(question, topK, minScore, null);
    }

    /**
     * 根据问题检索制度并生成可解释回答，支持指定检索参数和管线选项。
     *
     * @param question 用户问题
     * @param topK 召回 TopK 数量，为空时由实现按默认值处理
     * @param minScore 向量相似度最低阈值，为空时由实现按默认值处理
     * @param options 本次请求的 RAG 管线选项，为空时使用后端默认轻量模式
     * @return 知识库回答
     */
    RagResponse ask(String question, Integer topK, Double minScore, RagPipelineOptions options);
}
