package com.flowmind.rag.service.impl;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词提取服务。
 *
 * 用于从用户问题中提取适合关键词检索的内容。
 * 例如：
 * "BX2026001 是什么状态？" → ["BX2026001"]
 */
@Service
public class KeywordExtractService {

    /**
     * 编号类关键词正则。
     *
     * 适合提取：
     * BX2026001
     * AP2026001
     * PO2026001
     */
    private static final Pattern CODE_PATTERN =
            Pattern.compile("[A-Za-z]{2,}\\d{4,}");

    /**
     * 从问题中提取关键词。
     *
     * @param question 用户原始问题
     * @return 关键词列表
     */
    public List<String> extract(String question) {
        List<String> keywords = new ArrayList<>();

        if (question == null || question.isBlank()) {
            return keywords;
        }

        Matcher matcher = CODE_PATTERN.matcher(question);

        while (matcher.find()) {
            keywords.add(matcher.group());
        }

        return keywords;
    }
}