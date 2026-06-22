package com.flowmind.rag.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "knowledge_config")
public class KnowledgeConfig {

    @Id
    private Long id = 1L;

    private String name;

    private String adapterType;

    private Integer topK;

    private Double minScore;

    private String embeddingModel;

    private String rerankModel;

    private Integer chunkSize;

    private Integer chunkOverlap;

    @Column(name = "rag_flow_enabled")
    private Boolean ragFlowEnabled;

    private String ragFlowEndpoint;

    private String ragFlowDatasetId;

    private String ragFlowApiKey;
}