package com.flowmind.workflow.engine.model;

import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowInstance;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程执行上下文。
 *
 * 执行器和各类节点处理器通过该对象共享流程定义、实例、业务数据和流程图。
 */
@Data
public class WorkflowExecutionContext {

    /**
     * 流程定义快照。
     */
    private WorkflowDefinition definition;

    /**
     * 当前运行的流程实例。
     */
    private WorkflowInstance instance;

    /**
     * 用户提交的动态表单数据。
     */
    private Map<String, Object> businessData = new LinkedHashMap<>();

    /**
     * 解析后的流程图。
     */
    private WorkflowGraph graph;
}
