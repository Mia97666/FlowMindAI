package com.flowmind.rag.service.impl;

import com.flowmind.chat.ChatService;
import com.flowmind.common.exception.BusinessException;
import com.flowmind.common.exception.ErrorCode;
import com.flowmind.rag.config.RagPipelineProperties;
import com.flowmind.rag.dto.RagPipelineOptions;
import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.dto.RagSource;
import com.flowmind.rag.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 管线服务。
 * <p>
 * 负责串联：
 * 1. 查询重写：把口语化问题改成更适合检索的问题，可选，默认关闭。
 * 2. 多查询生成：生成多个等价检索表达，可选，默认关闭。
 * 3. 关键词提取：提取编号、金额等适合精确匹配的关键词。
 * 4. 混合检索：向量检索 + 关键词检索，合并去重。
 * 5. 上下文压缩：过滤低分片段、截断过长片段。
 * 6. 重排序：对候选 Chunk 重新排序，选择最终上下文。
 * 7. Prompt 构建：把问题和来源片段组装成模型提示词。
 * 8. 最终答案生成：调用大模型生成回答。
 * 9. 来源返回：返回 answer 和 sources，便于前端展示和审计追踪。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipelineService {

    private final RetrieverService retrieverService;
    private final RagPromptBuilder promptBuilder;
    private final ChatService chatService;
    private final QueryRewriteService queryRewriteService;
    private final MultiQueryService multiQueryService;
    private final KeywordExtractService keywordExtractService;
    private final ContextCompressionService contextCompressionService;
    private final RerankService rerankService;
    private final RagPipelineProperties properties;
    private final Map<CacheKey, CacheEntry> responseCache = new LinkedHashMap<>(16, 0.75F, true);

    /**
     * 执行完整 RAG 问答流程（使用默认检索参数）。
     *
     * @param question 用户问题
     * @return 带来源的 RAG 回答
     */
    public RagResponse ask(String question) {
        return ask(question, null, null);
    }

    /**
     * 执行完整 RAG 问答流程，支持指定检索参数。
     *
     * @param question 用户问题
     * @param topK     召回 TopK 数量，为空时使用默认值
     * @param minScore 向量相似度最低阈值，为空时使用默认值
     * @return 带来源的 RAG 回答
     */
    public RagResponse ask(String question, Integer topK, Double minScore) {
        return ask(question, topK, minScore, null);
    }

    /**
     * 执行完整 RAG 问答流程，支持指定检索参数和本次请求的管线选项。
     *
     * @param question 用户问题
     * @param topK 召回 TopK 数量，为空时使用默认值
     * @param minScore 向量相似度最低阈值，为空时使用默认值
     * @param options 本次请求的管线选项；为空时使用后端默认轻量模式
     * @return 带来源的 RAG 回答
     */
    public RagResponse ask(String question, Integer topK, Double minScore, RagPipelineOptions options) {
        Instant totalStart = Instant.now();
        int effectiveTopK = normalizeTopK(topK);
        double effectiveMinScore = normalizeMinScore(minScore);
        EffectiveOptions effectiveOptions = resolveOptions(options);
        CacheKey cacheKey = new CacheKey(
                question,
                effectiveTopK,
                effectiveMinScore,
                effectiveOptions.retrievalMode(),
                effectiveOptions.queryRewriteEnabled(),
                effectiveOptions.multiQueryEnabled()
        );
        RagResponse cachedResponse = getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            logCacheHit(question, effectiveTopK, effectiveMinScore, effectiveOptions, elapsed(totalStart));
            return cachedResponse;
        }

        QueryPlan queryPlan = buildQueryPlan(question, effectiveOptions);

        // 4. 混合检索：向量检索负责语义相似召回，关键词检索负责精确命中编号/金额等信息。
        Instant retrieveStart = Instant.now();
        List<RetrievedChunk> chunks;
        try {
            chunks = retrieverService.hybridRetrieve(
                    queryPlan.queries(),
                    queryPlan.keywords(),
                    effectiveTopK,
                    effectiveMinScore
            );
        } catch (BusinessException e) {
            // 向量检索底层失败（如缺少 API Key）统一以检索上下文异常码抛出，保留原始 cause
            throw new BusinessException(ErrorCode.HYBRID_RETRIEVE_API_KEY_MISSING, e);
        }
        long retrieveMs = elapsed(retrieveStart);
        int rawChunkCount = chunks.size();

        // 5. 上下文压缩：过滤低相关内容并限制上下文体积，降低最终回答的噪音和 token 消耗。
        Instant compressionStart = Instant.now();
        if (properties.isContextCompressionEnabled()) {
            chunks = contextCompressionService.compress(chunks);
        }
        long compressionMs = elapsed(compressionStart);

        // 6. 重排序：对召回候选按相关性重新排序，取最适合回答的 TopK。
        Instant rerankStart = Instant.now();
        if (properties.isRerankEnabled()) {
            chunks = rerankService.rerank(question, chunks, effectiveTopK);
        }
        long rerankMs = elapsed(rerankStart);

        // 7. Prompt 构建：把用户原始问题和最终上下文拼成提示词，避免回答偏离原意。
        Instant promptBuildStart = Instant.now();
        String prompt = promptBuilder.build(question, chunks);
        long promptBuildMs = elapsed(promptBuildStart);

        // 8. 最终答案生成：调用大模型基于 Prompt 输出自然语言答案。
        Instant answerStart = Instant.now();
        String answer = chatService.chat(prompt);
        long answerMs = elapsed(answerStart);

        List<RagSource> sources = chunks.stream()
                .map(chunk -> new RagSource(
                        chunk.getDocumentId(),
                        chunk.getDocumentName(),
                        chunk.getChunkId(),
                        chunk.getChunkIndex(),
                        chunk.getScore(),
                        chunk.getContent()
                ))
                .toList();
        logTimings(
                question,
                queryPlan,
                effectiveTopK,
                effectiveMinScore,
                rawChunkCount,
                chunks.size(),
                retrieveMs,
                compressionMs,
                rerankMs,
                promptBuildMs,
                answerMs,
                elapsed(totalStart)
        );

        RagResponse response = new RagResponse(answer, sources);
        putCachedResponse(cacheKey, response);
        return response;
    }

    private QueryPlan buildQueryPlan(String question, EffectiveOptions options) {
        List<String> queries = new ArrayList<>();
        queries.add(question);

        String rewrittenQuestion = question;
        long queryRewriteMs = 0;
        if (options.queryRewriteEnabled()) {
            // 1. 查询重写：把口语化问题改写成更适合知识库检索的查询语句。
            Instant queryRewriteStart = Instant.now();
            rewrittenQuestion = queryRewriteService.rewrite(question).trim();
            queryRewriteMs = elapsed(queryRewriteStart);
            if (!rewrittenQuestion.isBlank() && !containsIgnoreCase(queries, rewrittenQuestion)) {
                queries.add(rewrittenQuestion);
            }
        }

        long multiQueryMs = 0;
        if (options.multiQueryEnabled()) {
            // 2. 多查询生成：用不同表达方式扩大召回范围，适合制度问法不确定的场景。
            Instant multiQueryStart = Instant.now();
            multiQueryService.generateQueries(question).stream()
                    .filter(query -> query != null && !query.isBlank())
                    .filter(query -> !containsIgnoreCase(queries, query))
                    .limit(Math.max(0, properties.getMaxMultiQueries()))
                    .forEach(queries::add);
            multiQueryMs = elapsed(multiQueryStart);
        }

        // 3. 关键词提取：抽取编号、单号等适合 LIKE/全文检索的精确关键词。
        Instant keywordExtractStart = Instant.now();
        List<String> keywords = properties.isKeywordSearchEnabled()
                ? keywordExtractService.extract(question)
                : List.of();
        long keywordExtractMs = elapsed(keywordExtractStart);

        return new QueryPlan(
                rewrittenQuestion,
                queries,
                keywords,
                options.retrievalMode(),
                options.queryRewriteEnabled(),
                options.multiQueryEnabled(),
                queryRewriteMs,
                multiQueryMs,
                keywordExtractMs
        );
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return Math.max(1, properties.getDefaultTopK());
        }
        return topK;
    }

    private double normalizeMinScore(Double minScore) {
        if (minScore == null || minScore < 0) {
            return properties.getDefaultMinScore();
        }
        return minScore;
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(candidate));
    }

    private void logTimings(
            String question,
            QueryPlan queryPlan,
            int topK,
            double minScore,
            int rawChunkCount,
            int finalChunkCount,
            long retrieveMs,
            long compressionMs,
            long rerankMs,
            long promptBuildMs,
            long answerMs,
            long totalMs
    ) {
        if (!properties.isTimingLogEnabled()) {
            return;
        }
        log.info(
                "RAG pipeline finished questionChars={}, retrievalMode={}, topK={}, minScore={}, queries={}, keywords={}, rawChunks={}, finalChunks={}, rewriteEnabled={}, multiQueryEnabled={}, queryRewriteMs={}, multiQueryMs={}, keywordExtractMs={}, hybridRetrieveMs={}, compressionMs={}, rerankMs={}, promptBuildMs={}, answerMs={}, totalMs={}",
                question == null ? 0 : question.length(),
                queryPlan.retrievalMode(),
                topK,
                minScore,
                queryPlan.queries().size(),
                queryPlan.keywords().size(),
                rawChunkCount,
                finalChunkCount,
                queryPlan.queryRewriteEnabled(),
                queryPlan.multiQueryEnabled(),
                queryPlan.queryRewriteMs(),
                queryPlan.multiQueryMs(),
                queryPlan.keywordExtractMs(),
                retrieveMs,
                compressionMs,
                rerankMs,
                promptBuildMs,
                answerMs,
                totalMs
        );
        log.debug("RAG query plan rewrittenQuestion={}, queries={}, keywords={}",
                queryPlan.rewrittenQuestion(),
                queryPlan.queries(),
                queryPlan.keywords()
        );
    }

    private RagResponse getCachedResponse(CacheKey key) {
        if (!properties.isResponseCacheEnabled()) {
            return null;
        }
        synchronized (responseCache) {
            CacheEntry entry = responseCache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired(properties.getResponseCacheTtlSeconds())) {
                responseCache.remove(key);
                return null;
            }
            return entry.response();
        }
    }

    private void putCachedResponse(CacheKey key, RagResponse response) {
        if (!properties.isResponseCacheEnabled()) {
            return;
        }
        synchronized (responseCache) {
            responseCache.put(key, new CacheEntry(response, Instant.now()));
            trimCache();
        }
    }

    private void trimCache() {
        int maxSize = Math.max(0, properties.getResponseCacheMaxSize());
        while (responseCache.size() > maxSize) {
            CacheKey eldestKey = responseCache.keySet().iterator().next();
            responseCache.remove(eldestKey);
        }
    }

    private void logCacheHit(
            String question,
            int topK,
            double minScore,
            EffectiveOptions options,
            long totalMs
    ) {
        if (!properties.isTimingLogEnabled()) {
            return;
        }
        log.info(
                "RAG pipeline cache hit questionChars={}, retrievalMode={}, topK={}, minScore={}, rewriteEnabled={}, multiQueryEnabled={}, totalMs={}",
                question == null ? 0 : question.length(),
                options.retrievalMode(),
                topK,
                minScore,
                options.queryRewriteEnabled(),
                options.multiQueryEnabled(),
                totalMs
        );
    }

    private EffectiveOptions resolveOptions(RagPipelineOptions options) {
        if (options == null || options.getRetrievalMode() == null || options.getRetrievalMode().isBlank()) {
            boolean rewriteEnabled = properties.isQueryRewriteEnabled();
            boolean multiQueryEnabled = properties.isMultiQueryEnabled();
            return new EffectiveOptions(
                    rewriteEnabled || multiQueryEnabled ? "HIGH_RECALL" : "LIGHT",
                    rewriteEnabled,
                    multiQueryEnabled
            );
        }
        String mode = options.getRetrievalMode().trim().toUpperCase();
        if (!"HIGH_RECALL".equals(mode)) {
            return new EffectiveOptions("LIGHT", false, false);
        }
        return new EffectiveOptions(
                "HIGH_RECALL",
                Boolean.TRUE.equals(options.getQueryRewriteEnabled()),
                Boolean.TRUE.equals(options.getMultiQueryEnabled())
        );
    }

    private long elapsed(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }

    private record QueryPlan(
            String rewrittenQuestion,
            List<String> queries,
            List<String> keywords,
            String retrievalMode,
            boolean queryRewriteEnabled,
            boolean multiQueryEnabled,
            long queryRewriteMs,
            long multiQueryMs,
            long keywordExtractMs
    ) {
    }

    private record EffectiveOptions(
            String retrievalMode,
            boolean queryRewriteEnabled,
            boolean multiQueryEnabled
    ) {
    }

    private record CacheKey(
            String question,
            int topK,
            double minScore,
            String retrievalMode,
            boolean queryRewriteEnabled,
            boolean multiQueryEnabled
    ) {

        private CacheKey {
            question = question == null ? "" : question.trim();
            retrievalMode = retrievalMode == null ? "LIGHT" : retrievalMode;
        }
    }

    private record CacheEntry(RagResponse response, Instant createdAt) {

        private boolean isExpired(long ttlSeconds) {
            if (ttlSeconds <= 0) {
                return true;
            }
            return Duration.between(createdAt, Instant.now()).getSeconds() >= ttlSeconds;
        }
    }
}
