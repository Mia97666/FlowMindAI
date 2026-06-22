package com.flowmind.rag.service.impl;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.flowmind.common.exception.BusinessException;
import com.flowmind.common.exception.ErrorCode;
import dev.langchain4j.data.embedding.Embedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.embedding-model}")
    private String embeddingModel;

    public Embedding embed(String text) {
        TextEmbeddingParam param = TextEmbeddingParam.builder()
                .apiKey(apiKey)
                .model(embeddingModel)
                .text(text)
                .build();

        TextEmbedding textEmbedding = new TextEmbedding();

        TextEmbeddingResult result;
        try {
            result = textEmbedding.call(param);
        } catch (NoApiKeyException e) {
            throw new BusinessException(ErrorCode.EMBEDDING_API_KEY_MISSING, e);
        }

        List<Double> vector = result.getOutput().getEmbeddings().get(0).getEmbedding();

        float[] floats = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            floats[i] = vector.get(i).floatValue();
        }

        return Embedding.from(floats);
    }
}