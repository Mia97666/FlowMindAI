package com.flowmind.rag.service.impl;

import com.flowmind.chat.ChatService;
import com.flowmind.common.exception.BusinessException;
import com.flowmind.common.exception.ErrorCode;
import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.dto.RagSource;
import com.flowmind.rag.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 管线服务。
 * <p>
 * 负责串联：
 * 1. 检索
 * 2. Prompt 构建
 * 3. 大模型回答
 * 4. 来源返回
 */
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

    /**
     * 默认召回 TopK 数量。
     */
    private static final int DEFAULT_TOP_K = 20;

    /**
     * 默认向量相似度最低阈值。
     */
    private static final double DEFAULT_MIN_SCORE = 0.4;

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
        int effectiveTopK = topK != null ? topK : DEFAULT_TOP_K;
        double effectiveMinScore = minScore != null ? minScore : DEFAULT_MIN_SCORE;

        //提高召回率，查询重写：把口语化问题改写成更适合检索的问题
        String rewrittenQuestion = queryRewriteService.rewrite(question);

        //提高召回率，多查询生成：单问题分解成多问题,用不同表达方式扩大召回范围
        List<String> queries = new ArrayList<>();
        queries.add(rewrittenQuestion);
        queries.addAll(multiQueryService.generateQueries(question));

        //关键词检索：从问题中提取关键词
        List<String> keywords = keywordExtractService.extract(question);

        //混合检索：向量多路召回+关键词检索，合并去重并加权
        List<RetrievedChunk> chunks;
        try {
            chunks = retrieverService.hybridRetrieve(
                    queries,
                    keywords,
                    effectiveTopK,
                    effectiveMinScore
            );
        } catch (BusinessException e) {
            // 向量检索底层失败（如缺少 API Key）统一以检索上下文异常码抛出，保留原始 cause
            throw new BusinessException(ErrorCode.HYBRID_RETRIEVE_API_KEY_MISSING, e);
        }
        System.out.println("压缩前数量：" + chunks.size());

        //提高准确率：上下文压缩降噪，减去无用信息
        chunks = contextCompressionService.compress(chunks);
        System.out.println("压缩后数量：" + chunks.size());

        //Qwen Rerank：对候选 Chunk 重新排序，按前端 topK 取最相关结果
        chunks = rerankService.rerank(question, chunks, effectiveTopK);

        //构建 Prompt：仍然使用用户原始问题，避免回答偏离
        String prompt = promptBuilder.build(question, chunks);

        //调用模型，将提示词传给大模型，让大模型回答问题
        String answer = chatService.chat(prompt);

        //返回来源，便于前端展示和审计追踪
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
        System.out.println("原始问题：" + question);
        System.out.println("改写问题：" + rewrittenQuestion);
        System.out.println("多查询：" + queries);
        System.out.println("召回数量：" + chunks.size());

        return new RagResponse(answer, sources);
    }
}