package com.flowmind.rag.service.impl;

import com.flowmind.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 查询重写服务。
 *
 * 作用：
 * 将用户的口语化、模糊问题，改写成更适合知识库检索的问题。
 */
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private final ChatService chatService;

    /**
     * 重写用户问题。
     *
     * @param question 用户原始问题
     * @return 更适合检索的规范化问题
     */
    public String rewrite(String question) {
        String prompt = """
                你是企业知识库检索优化助手。

                请将用户问题改写成一个更适合知识库检索的查询语句。

                要求：
                1. 保留用户原始意图。
                2. 不要回答问题。
                3. 不要扩展不存在的信息。
                4. 只输出改写后的查询语句。
                5. 如果原问题已经清晰，则原样返回。

                用户问题：
                %s
                """.formatted(question);

        return chatService.chat(prompt).trim();
    }
}