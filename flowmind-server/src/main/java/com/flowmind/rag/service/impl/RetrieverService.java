package com.flowmind.rag.service.impl;

import com.flowmind.rag.dto.RetrievedChunk;
import com.flowmind.rag.entity.KnowledgeChunk;
import com.flowmind.rag.entity.KnowledgeDocument;
import com.flowmind.rag.repository.KnowledgeChunkRepository;
import com.flowmind.rag.repository.KnowledgeDocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检索服务。
 *
 * 当前版本只做向量检索。
 * 后续会在这里扩展：
 * 1. 关键词检索
 * 2. 混合检索
 * 3. 多路召回
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieverService {

    private final EmbeddingService embeddingService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;

    /**
     * 根据用户问题进行向量检索。
     *
     * @param question 用户问题
     * @param topK 最大召回数量
     * @param minScore 最低相似度分数
     * @return 召回到的 Chunk 列表
     */
    public List<RetrievedChunk> retrieve(String question, int topK, double minScore) {
        Embedding questionEmbedding = embeddingService.embed(question);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(topK)
                .minScore(minScore)
                .build();

        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore.search(searchRequest).matches();

        if (matches.isEmpty()) {
            return List.of();
        }

        Map<String, EmbeddingMatch<TextSegment>> matchesByVectorId = matches.stream()
                .filter(match -> match.embeddingId() != null)
                .collect(Collectors.toMap(
                        EmbeddingMatch::embeddingId,
                        Function.identity(),
                        (left, right) -> left.score() >= right.score() ? left : right,
                        LinkedHashMap::new
                ));

        List<KnowledgeChunk> matchedChunks = chunkRepository.findByVectorIdIn(
                new ArrayList<>(matchesByVectorId.keySet())
        );
        Map<Long, KnowledgeDocument> documentsById = loadDocuments(matchedChunks);

        List<RetrievedChunk> results = new ArrayList<>();
        for (KnowledgeChunk chunk : matchedChunks) {
            EmbeddingMatch<TextSegment> match = matchesByVectorId.get(chunk.getVectorId());
            if (match == null) {
                continue;
            }
            KnowledgeDocument document = documentsById.get(chunk.getDocumentId());
            String documentName = document == null ? "未知文档" : document.getOriginalFilename();

            results.add(new RetrievedChunk(
                    chunk.getDocumentId(),
                    documentName,
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    match.score(),
                    chunk.getContent(),
                    "VECTOR"
            ));
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .toList();
    }

    /**
     * 根据多个查询进行检索，并按 chunkId 去重。
     *
     * @param queries 多个检索查询
     * @param topK 每个查询召回数量
     * @param minScore 最低相似度
     * @return 去重后的召回结果
     */
    public List<RetrievedChunk> retrieveMulti(List<String> queries, int topK, double minScore){
        Map<Long, RetrievedChunk> dedupMap = new LinkedHashMap<>();

        for (String query : queries) {
            List<RetrievedChunk> chunks = retrieve(query, topK, minScore);

            for (RetrievedChunk chunk : chunks) {
                RetrievedChunk existing = dedupMap.get(chunk.getChunkId());

                if (existing == null || chunk.getScore() > existing.getScore()) {
                    dedupMap.put(chunk.getChunkId(), chunk);
                }
            }
        }

        return dedupMap.values()
                .stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .toList();
    }

    /**
     * 根据关键词检索 Chunk。
     *
     * 当前版本使用 PostgreSQL LIKE。
     * 适合匹配金额、编号、制度关键词等精确文本。
     *
     * @param keyword 检索关键词
     * @param topK 最大返回数量
     * @return 关键词召回结果
     */
    public List<RetrievedChunk> retrieveByKeyword(String keyword, int topK) {
        List<KnowledgeChunk> chunks = chunkRepository.searchByKeyword(keyword);
        Map<Long, KnowledgeDocument> documentsById = loadDocuments(chunks);

        return chunks.stream()
                .limit(topK)
                .map(chunk -> {
                    KnowledgeDocument document = documentsById.get(chunk.getDocumentId());

                    String documentName = document == null
                            ? "未知文档"
                            : document.getOriginalFilename();

                    return new RetrievedChunk(
                            chunk.getDocumentId(),
                            documentName,
                            chunk.getId(),
                            chunk.getChunkIndex(),
                            0.75,
                            chunk.getContent(),
                            "KEYWORD"
                    );
                })
                .toList();
    }
    /**
     * 根据多个关键词检索 Chunk。
     *
     * @param keywords 关键词列表
     * @param topK 最大返回数量
     * @return 关键词召回结果
     */
    public List<RetrievedChunk> retrieveByKeywords(List<String> keywords, int topK) {
        Map<Long, RetrievedChunk> dedupMap = new LinkedHashMap<>();

        for (String keyword : keywords) {
            List<RetrievedChunk> chunks = retrieveByKeyword(keyword, topK);

            for (RetrievedChunk chunk : chunks) {
                RetrievedChunk existing = dedupMap.get(chunk.getChunkId());

                if (existing == null || chunk.getScore() > existing.getScore()) {
                    dedupMap.put(chunk.getChunkId(), chunk);
                }
            }
        }

        return dedupMap.values()
                .stream()
                .limit(topK)
                .toList();
    }

    /**
     * 混合检索。
     *
     * 流程：
     * 1. 多查询向量检索
     * 2. 关键词检索
     * 3. 按 chunkId 合并去重
     * 4. 按分数排序
     *
     * @param queries 多个向量检索查询
     * @param keywords 多关键词检索词
     * @param topK 最大返回数量
     * @param minScore 向量检索最低分
     * @return 混合召回结果
     */
    public List<RetrievedChunk> hybridRetrieve(
            List<String> queries,
            List<String> keywords,
            int topK,
            double minScore
    ) {
        Map<Long, RetrievedChunk> dedupMap = new LinkedHashMap<>();

        // 1. 向量多路召回
        List<RetrievedChunk> vectorResults =
                retrieveMulti(queries, topK, minScore);

        for (RetrievedChunk chunk : vectorResults) {
            dedupMap.put(chunk.getChunkId(), chunk);
            log.debug("Vector retrieved chunkId={}, score={}", chunk.getChunkId(), chunk.getScore());
        }

        // 2. 关键词召回
        List<RetrievedChunk> keywordResults =
                retrieveByKeywords(keywords, topK);

        for (RetrievedChunk chunk : keywordResults) {
            RetrievedChunk existing = dedupMap.get(chunk.getChunkId());

            if (existing == null) {
                dedupMap.put(chunk.getChunkId(), chunk);
            } else {
                // 如果同一个 chunk 同时被向量和关键词命中，提升分数
                existing.setScore(Math.max(existing.getScore(), chunk.getScore()) + 0.1);
                existing.setSource("HYBRID");
            }
        }

        // 3. 排序截断
        return dedupMap.values()
                .stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .toList();
    }

    private Map<Long, KnowledgeDocument> loadDocuments(List<KnowledgeChunk> chunks) {
        List<Long> documentIds = chunks.stream()
                .map(KnowledgeChunk::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (documentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(
                        KnowledgeDocument::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }
}
