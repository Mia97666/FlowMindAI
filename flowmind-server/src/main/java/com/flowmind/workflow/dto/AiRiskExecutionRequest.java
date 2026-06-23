package com.flowmind.workflow.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 风险检测执行上下文。
 *
 * 工作流节点执行时会把节点配置一并传入，便于按策略、RAG 参数和阈值执行。
 */
@Data
public class AiRiskExecutionRequest {

    private String workflowCode;

    private String nodeId;

    private String nodeName;

    private Map<String, Object> nodeConfig = new LinkedHashMap<>();

    private Map<String, Object> businessData = new LinkedHashMap<>();
}
