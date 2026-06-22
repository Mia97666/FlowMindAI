package com.flowmind.workflow.engine.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流节点。
 */
@Data
public class WorkflowNode {

    /**
     * 节点ID，需要在一个流程定义内唯一。
     */
    private String id;

    /**
     * 节点类型。
     *
     * 支持 START、FORM、AI_RISK_CHECK、AI_APPROVAL、APPROVAL、
     * CONDITION、EXCLUSIVE_GATEWAY、NOTIFY、END。
     */
    private String type;

    /**
     * 节点名称。
     */
    private String name;

    /**
     * 节点配置。
     *
     * 不同类型节点有不同配置，例如审批人、风险阈值、通知接收人等。
     */
    private Map<String, Object> config = new LinkedHashMap<>();
}
