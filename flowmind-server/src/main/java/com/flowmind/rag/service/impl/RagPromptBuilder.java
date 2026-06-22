package com.flowmind.rag.service.impl;

import com.flowmind.rag.dto.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG Prompt 构建器。
 *
 * 负责把召回的知识片段组装成适合大模型阅读的上下文。
 */
@Component
public class RagPromptBuilder {

    /**
     * 构建 RAG 问答 Prompt。
     *
     * @param question 用户问题
     * @param chunks 召回的知识片段
     * @return 发送给大模型的完整 Prompt
     */
    public String build(String question, List<RetrievedChunk> chunks) {
        StringBuilder contextBuilder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);

            contextBuilder.append("[来源")
                    .append(i + 1)
                    .append("]\n")
                    .append("文档：")
                    .append(chunk.getDocumentName())
                    .append("\n")
                    .append("Chunk：")
                    .append(chunk.getChunkIndex())
                    .append("\n")
                    .append("内容：")
                    .append(chunk.getContent())
                    .append("\n\n");
        }

        return """
                你是 FlowMind AI 企业审批知识库助手。

                请严格遵守以下规则：
                1. 只能根据【知识库内容】回答。
                2. 如果知识库内容不足以回答，请回答：根据当前知识库无法确定。
                3. 回答中必须引用来源编号，例如：[来源1]。
                4. 不要编造制度、金额、审批人或流程节点。

                【知识库内容】
                %s

                【用户问题】
                %s
                """.formatted(contextBuilder, question);
    }
}