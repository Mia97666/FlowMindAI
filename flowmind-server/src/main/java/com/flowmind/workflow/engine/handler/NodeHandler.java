package com.flowmind.workflow.engine.handler;

import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;

/**
 * 节点处理器策略接口。
 *
 * 每种节点类型只关心自己的业务动作，流转控制由 WorkflowExecutionService 统一负责。
 */
public interface NodeHandler {

    /**
     * 判断该处理器是否支持指定节点类型。
     */
    boolean support(String nodeType);

    /**
     * 执行节点动作。
     */
    NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    );
}
