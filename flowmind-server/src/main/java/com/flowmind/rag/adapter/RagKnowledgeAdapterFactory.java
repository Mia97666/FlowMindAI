package com.flowmind.rag.adapter;

import com.flowmind.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RAG 知识库适配器工厂。
 *
 * 根据前端传入的 adapterType 路由到对应适配器实现，
 * 使流程层无需感知具体知识库类型。
 */
@Component
@RequiredArgsConstructor
public class RagKnowledgeAdapterFactory {

    private final SelfRagKnowledgeAdapter selfRagKnowledgeAdapter;

    private final RagFlowKnowledgeAdapter ragFlowKnowledgeAdapter;

    /**
     * 默认适配器类型。
     */
    private static final String DEFAULT_ADAPTER_TYPE = "SELF";

    /**
     * 根据适配器类型获取对应实现。
     *
     * @param adapterType 适配器类型，为空时默认 SELF
     * @return 知识库适配器
     */
    public RagKnowledgeAdapter get(String adapterType) {
        String type = adapterType == null || adapterType.isBlank()
                ? DEFAULT_ADAPTER_TYPE
                : adapterType.trim().toUpperCase();

        return switch (type) {
            case "SELF" -> selfRagKnowledgeAdapter;
            case "RAGFLOW" -> ragFlowKnowledgeAdapter;
            default -> throw new BadRequestException("不支持的知识库适配器类型：" + adapterType);
        };
    }
}
