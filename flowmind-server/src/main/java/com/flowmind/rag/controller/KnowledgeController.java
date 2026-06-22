package com.flowmind.rag.controller;

import com.flowmind.rag.dto.KnowledgeRequest;
import com.flowmind.rag.entity.KnowledgeConfig;
import com.flowmind.rag.repository.KnowledgeConfigRepository;
import com.flowmind.rag.service.impl.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final EmbeddingService embeddingService;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final KnowledgeConfigRepository configRepository;

    private final EntityManager entityManager;

    @PostMapping
    public Map<String, Object> addKnowledge(@Valid @RequestBody KnowledgeRequest request){
        Embedding embedding = embeddingService.embed(request.getContent());

        TextSegment segment = TextSegment.from(request.getContent());

        String id = embeddingStore.add(embedding, segment);

        return Map.of(
                "id", id,
                "content", request.getContent(),
                "message", "知识写入成功"
        );
    }

    @GetMapping("/config")
    @Transactional
    public KnowledgeConfig getConfig() {
        KnowledgeConfig config = configRepository.findById(1L).orElse(null);
        if (config == null) {
            config = new KnowledgeConfig();
            config.setName("FlowMind 知识库");
            config.setAdapterType("SELF");
            config.setTopK(8);
            config.setMinScore(0.55);
            config.setEmbeddingModel("text-embedding-v3");
            config.setRerankModel("bge-reranker-v2-m3");
            config.setChunkSize(800);
            config.setChunkOverlap(120);
            config.setRagFlowEnabled(false);
            entityManager.persist(config);
        }
        return config;
    }

    @PostMapping("/config")
    @Transactional
    public KnowledgeConfig saveConfig(@RequestBody KnowledgeConfig request) {
        KnowledgeConfig config = configRepository.findById(1L).orElse(null);
        if (config == null) {
            config = new KnowledgeConfig();
            copyNonNull(request, config);
            entityManager.persist(config);
            return config;
        }
        copyNonNull(request, config);
        return configRepository.save(config);
    }

    private void copyNonNull(KnowledgeConfig source, KnowledgeConfig target) {
        if (source.getName() != null) target.setName(source.getName());
        if (source.getAdapterType() != null) target.setAdapterType(source.getAdapterType());
        if (source.getTopK() != null) target.setTopK(source.getTopK());
        if (source.getMinScore() != null) target.setMinScore(source.getMinScore());
        if (source.getEmbeddingModel() != null) target.setEmbeddingModel(source.getEmbeddingModel());
        if (source.getRerankModel() != null) target.setRerankModel(source.getRerankModel());
        if (source.getChunkSize() != null) target.setChunkSize(source.getChunkSize());
        if (source.getChunkOverlap() != null) target.setChunkOverlap(source.getChunkOverlap());
        if (source.getRagFlowEnabled() != null) target.setRagFlowEnabled(source.getRagFlowEnabled());
        if (source.getRagFlowEndpoint() != null) target.setRagFlowEndpoint(source.getRagFlowEndpoint());
        if (source.getRagFlowDatasetId() != null) target.setRagFlowDatasetId(source.getRagFlowDatasetId());
        if (source.getRagFlowApiKey() != null) target.setRagFlowApiKey(source.getRagFlowApiKey());
    }
}