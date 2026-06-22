package com.flowmind.rag.service.impl;

import com.flowmind.rag.dto.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文压缩服务。降噪
 *
 * 作用：
 * 1. 去掉低分 Chunk
 * 2. 按 chunkId 去重
 * 3. 限制最终上下文数量
 * 4. 截断过长内容
 */
@Service
public class ContextCompressionService {

    private static final int MAX_CONTEXT_CHUNKS = 20;

    private static final int MAX_CHUNK_LENGTH = 800;

    private static final double MIN_SCORE = 0.6;

    public List<RetrievedChunk> compress(List<RetrievedChunk> chunks) {
        Map<Long, RetrievedChunk> dedupMap = new LinkedHashMap<>();

        for (RetrievedChunk chunk : chunks) {
            if (chunk.getScore() < MIN_SCORE) {
                continue;
            }

            String content = chunk.getContent();

            if (content != null && content.length() > MAX_CHUNK_LENGTH) {
                chunk.setContent(content.substring(0, MAX_CHUNK_LENGTH));
            }

            RetrievedChunk existing = dedupMap.get(chunk.getChunkId());

            if (existing == null || chunk.getScore() > existing.getScore()) {
                dedupMap.put(chunk.getChunkId(), chunk);
            }
        }

        return dedupMap.values()
                .stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(MAX_CONTEXT_CHUNKS)
                .toList();
    }
}
