package com.flowmind.rag.store;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.*;

public class QdrantRestEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final RestClient restClient;
    private final String collectionName;

    public QdrantRestEmbeddingStore(String host, int port, String collectionName) {
        this.restClient = RestClient.create();
        this.collectionName = collectionName;
    }

    private String baseUrl() {
        return "http://localhost:6333";
    }

    @Override
    public String add(Embedding embedding) {
        return add(embedding, null);
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return addAll(embeddings, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> points = new ArrayList<>();

        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            ids.add(id);

            Map<String, Object> point = new HashMap<>();
            point.put("id", id);
            point.put("vector", floatArrayToList(embeddings.get(i).vector()));

            if (textSegments != null && i < textSegments.size() && textSegments.get(i) != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("text_segment", textSegments.get(i).text());
                point.put("payload", payload);
            }

            points.add(point);
        }

        Map<String, Object> body = Map.of("points", points);

        restClient.put()
                .uri(baseUrl() + "/collections/{collection}/points?wait=true", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        return ids;
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<Map<String, Object>> points = new ArrayList<>();

        for (int i = 0; i < embeddings.size(); i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("id", ids.get(i));
            point.put("vector", floatArrayToList(embeddings.get(i).vector()));

            if (textSegments != null && i < textSegments.size() && textSegments.get(i) != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("text_segment", textSegments.get(i).text());
                point.put("payload", payload);
            }

            points.add(point);
        }

        Map<String, Object> body = Map.of("points", points);

        restClient.put()
                .uri(baseUrl() + "/collections/{collection}/points?wait=true", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void remove(String id) {
        restClient.post()
                .uri(baseUrl() + "/collections/{collection}/points/delete", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("points", List.of(id)))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void removeAll(Collection<String> ids) {
        restClient.post()
                .uri(baseUrl() + "/collections/{collection}/points/delete", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("points", ids))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void removeAll() {
        restClient.post()
                .uri(baseUrl() + "/collections/{collection}/points/delete", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("filter", Map.of()))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", floatArrayToList(request.queryEmbedding().vector()));
        body.put("limit", request.maxResults());
        body.put("with_payload", true);
        body.put("with_vector", true);

        if (request.filter() != null) {
            // basic filter support can be added if needed
        }

        Map response = restClient.post()
                .uri(baseUrl() + "/collections/{collection}/points/search", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (Map<String, Object> result : results) {
            String id = (String) result.get("id");
            double score = ((Number) result.get("score")).doubleValue();

            List<Double> vectorData = (List<Double>) result.get("vector");
            float[] vector = new float[vectorData.size()];
            for (int i = 0; i < vectorData.size(); i++) {
                vector[i] = vectorData.get(i).floatValue();
            }
            Embedding embedding = Embedding.from(vector);

            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            String text = payload != null ? (String) payload.get("text_segment") : null;
            TextSegment textSegment = text != null ? TextSegment.from(text) : null;

            EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(
                    RelevanceScore.fromCosineSimilarity(score),
                    id,
                    embedding,
                    textSegment
            );

            if (request.minScore() > 0) {
                double minRelevance = RelevanceScore.fromCosineSimilarity(request.minScore());
                if (match.score() >= minRelevance) {
                    matches.add(match);
                }
            } else {
                matches.add(match);
            }
        }

        return new EmbeddingSearchResult<>(matches);
    }

    private void addInternal(String id, Embedding embedding, TextSegment textSegment) {
        Map<String, Object> point = new HashMap<>();
        point.put("id", id);
        point.put("vector", floatArrayToList(embedding.vector()));

        if (textSegment != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("text_segment", textSegment.text());
            point.put("payload", payload);
        }

        Map<String, Object> body = Map.of("points", List.of(point));

        restClient.put()
                .uri(baseUrl() + "/collections/{collection}/points?wait=true", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private static List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    @Override
    public void removeAll(dev.langchain4j.store.embedding.filter.Filter filter) {
        throw new UnsupportedOperationException("Filter-based removal not implemented");
    }
}