package com.flowmind.ai.controller.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 风险检测请求。
 */
@Data
public class AiRiskCheckRequest {

    /**
     * 流程编码。
     */
    private String workflowCode;

    /**
     * 业务数据。
     */
    private Map<String, Object> businessData = new LinkedHashMap<>();
}
