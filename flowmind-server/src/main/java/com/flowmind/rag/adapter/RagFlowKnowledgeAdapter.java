package com.flowmind.rag.adapter;

import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.dto.RagSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAGFlow 预留适配器。
 *
 * 对应前端 adapterType = RAGFLOW。
 *
 * 现在不主动注入使用，只保留统一接口和占位实现。
 * 后续接入外部 RAGFlow 时，可以在这里封装 RAGFlow HTTP API、
 * API Key、知识库 ID 和来源引用转换逻辑。
 */
@Component
public class RagFlowKnowledgeAdapter implements RagKnowledgeAdapter {

    @Override
    public RagResponse ask(String question, Integer topK, Double minScore) {
        return new RagResponse(
                "RAGFlow Adapter 已预留，当前环境仍使用自研 RAG Pipeline。",
                List.of(new RagSource(null, "RAGFlow Adapter", null, 0, 0D, question))
        );
    }
}
