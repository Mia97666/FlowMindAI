package com.flowmind.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RagResponse {

    private String answer;

    /**
     * 解释RAG来源
     */
    private List<RagSource> sources;
}