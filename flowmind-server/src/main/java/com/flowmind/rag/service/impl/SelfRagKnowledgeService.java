package com.flowmind.rag.service.impl;

import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.dto.RagPipelineOptions;
import com.flowmind.rag.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author mia
 */
@Service
@RequiredArgsConstructor
public class SelfRagKnowledgeService implements KnowledgeService {

    private final RagPipelineService ragPipelineService;

    @Override
    public RagResponse ask(String question, Integer topK, Double minScore, RagPipelineOptions options) {
        return ragPipelineService.ask(question, topK, minScore, options);
    }
}
