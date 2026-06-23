package com.flowmind.rag.controller;

import com.flowmind.rag.adapter.RagKnowledgeAdapter;
import com.flowmind.rag.adapter.RagKnowledgeAdapterFactory;
import com.flowmind.rag.dto.RagRequest;
import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.service.impl.RagRateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 问答接口。
 *
 * 根据前端传入的 adapterType 路由到对应知识库适配器，
 * 并将 topK / minScore 透传到深层检索逻辑。
 */
@RestController
@RequestMapping("/api/chat/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagKnowledgeAdapterFactory adapterFactory;
    private final RagRateLimiter ragRateLimiter;

    @PostMapping
    public RagResponse ragChat(@Valid @RequestBody RagRequest request) {
        // 测试阶段限流：检查并累加当日 RAG 调用配额，超限抛 BusinessException
        ragRateLimiter.checkAndIncrement();

        // 按 adapterType 选择知识库实现，默认 SELF
        RagKnowledgeAdapter adapter = adapterFactory.get(request.getAdapterType());

        // topK / minScore 透传到深层检索逻辑
        return adapter.ask(
                request.getQuestion(),
                request.getTopK(),
                request.getMinScore()
        );
    }
}
