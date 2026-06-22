package com.flowmind.rag.config;

import com.flowmind.rag.store.QdrantRestEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${qdrant.host}") String host,
            @Value("${qdrant.collection-name}") String collectionName
    ) {
        return new QdrantRestEmbeddingStore(host, 6333, collectionName);
    }
}