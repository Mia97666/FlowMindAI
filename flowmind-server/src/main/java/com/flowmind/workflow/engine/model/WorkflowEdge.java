package com.flowmind.workflow.engine.model;

import lombok.Data;

/**
 * 工作流连线。
 */
@Data
public class WorkflowEdge {

    /**
     * 连线ID。
     */
    private String id;

    /**
     * 来源节点ID。
     */
    private String source;

    /**
     * 目标节点ID。
     */
    private String target;

    /**
     * 条件表达式。
     *
     * 示例：
     * riskScore >= 70
     * riskLevel == "HIGH"
     * businessData.amount < 200000
     */
    private String condition;

    /**
     * 连线标签，主要用于前端展示。
     */
    private String label;
}
