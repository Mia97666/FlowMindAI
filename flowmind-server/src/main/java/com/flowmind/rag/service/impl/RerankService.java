package com.flowmind.rag.service.impl;

import com.flowmind.rag.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Qwen Rerank 重排序服务。
 *
 * 作用：
 * 在向量检索 / 混合检索召回 TopN 之后，
 * 使用 qwen3-rerank 对候选 Chunk 重新打分，
 * 再选出最相关的 TopK 作为最终上下文。
 */
@Service
@RequiredArgsConstructor
public class RerankService {

    /**
     * 重排序（假排，防止自测token消耗，真排序已被注释）
     *
     * @param question 用户问题
     * @param chunks 召回结果
     * @return 重排后结果
     */
    public List<RetrievedChunk> rerank(
            String question,
            List<RetrievedChunk> chunks, int topK
    ) {

        return chunks.stream()
                .sorted(
                        Comparator.comparingDouble(
                                chunk -> score(question, (RetrievedChunk) chunk)
                        ).reversed()
                )
                .limit(topK)
                .toList();
    }

    /**
     * 简单打分
     */
    private double score(
            String question,
            RetrievedChunk chunk
    ) {

        double score = chunk.getScore();

        String content = chunk.getContent();

        if (content.contains(question)) {
            score += 0.3;
        }

        return score;
    }

//
//    /**
//     * DashScope API Key。
//     * 从环境变量 DASHSCOPE_API_KEY 注入。
//     */
//    @Value("${dashscope.api-key}")
//    private String apiKey;
//
//    /**
//     * Rerank 模型名称，例如 qwen3-rerank。
//     */
//    @Value("${dashscope.rerank-model}")
//    private String rerankModel;
//
//    /**
//     * Rerank 接口地址。
//     */
//    @Value("${dashscope.rerank-url}")
//    private String rerankUrl;
//
//    private final RestClient restClient = RestClient.create();
//
//    /**
//     * 对召回结果进行重排序。真排序，会消耗token
//     *
//     * @param question 用户原始问题
//     * @param chunks   混合检索召回的候选 Chunk
//     * @param topK     重排序后保留的数量
//     * @return 重排序后的 Chunk 列表
//     */
//    public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> chunks, int topK) {
//        if (chunks == null || chunks.isEmpty()) {
//            return List.of();
//        }
//
//        // qwen3-rerank 最终接收的是候选文档内容数组
//        List<String> documents = chunks.stream()
//                .map(RetrievedChunk::getContent)
//                .toList();
//
//        Map<String, Object> requestBody = Map.of(
//                "model", rerankModel,
//                "query", question,
//                "documents", documents,
//                "top_n", Math.min(topK, documents.size()),
//                "return_documents", false
//        );
//
//        Map response = restClient.post()
//                .uri(rerankUrl)
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("Authorization", "Bearer " + apiKey)
//                .body(requestBody)
//                .retrieve()
//                .body(Map.class);
//
//        List<Map<String, Object>> results =
//                (List<Map<String, Object>>) response.get("results");
//
//        List<RetrievedChunk> rerankedChunks = new ArrayList<>();
//
//        for (Map<String, Object> item : results) {
//            Integer index = ((Number) item.get("index")).intValue();
//            Double relevanceScore = ((Number) item.get("relevance_score")).doubleValue();
//
//            RetrievedChunk chunk = chunks.get(index);
//
//            // 使用 Reranker 返回的相关性分数覆盖原始召回分数
//            chunk.setScore(relevanceScore);
//
//            // 标记该结果经过 Rerank 处理
//            chunk.setSource("RERANK");
//
//            rerankedChunks.add(chunk);
//        }
//
//        return rerankedChunks.stream()
//                .sorted(Comparator.comparingDouble(RetrievedChunk::getScore).reversed())
//                .limit(topK)
//                .toList();
//    }
}