package com.flowmind.rag.adapter;

import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 自研 RAG Pipeline 适配器。
 *
 * 对应前端 adapterType = SELF。
 */
@Primary
@Component
@RequiredArgsConstructor
public class SelfRagKnowledgeAdapter implements RagKnowledgeAdapter {

    private final KnowledgeService knowledgeService;

    @Override
    public RagResponse ask(String question, Integer topK, Double minScore) {
        return knowledgeService.ask(question, topK, minScore);
    }
}
