package com.flowmind.rag.service.impl;

import com.flowmind.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 多查询生成服务。
 *
 * 作用：
 * 根据用户问题生成多个不同表达方式的检索问题，
 * 用于提升 RAG 召回率。
 */
@Service
@RequiredArgsConstructor
public class MultiQueryService {

    private final ChatService chatService;

    /**
     * 根据用户问题生成多个检索查询。
     *
     * @param question 用户原始问题
     * @return 多个适合检索的查询语句
     */
    public List<String> generateQueries(String question) {
        String prompt = """
                你是企业知识库检索优化助手。

                请根据用户问题生成 3 个不同表达方式的检索查询。

                要求：
                1. 保留原始问题的业务意图。
                2. 不要回答问题。
                3. 不要编造不存在的信息。
                4. 每行只输出一个查询。
                5. 不要编号，不要解释。

                用户问题：
                %s
                """.formatted(question);

        String result = chatService.chat(prompt);

        return Arrays.stream(result.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(3)
                .toList();
    }
}