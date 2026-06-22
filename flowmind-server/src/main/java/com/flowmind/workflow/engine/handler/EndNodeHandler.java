package com.flowmind.workflow.engine.handler;

import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * 结束节点处理器。
 */
@Component
public class EndNodeHandler implements NodeHandler {

    @Override
    public boolean support(String nodeType) {
        return "END".equalsIgnoreCase(nodeType);
    }

    @Override
    public NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    ) {
        return NodeExecutionResult.completed();
    }
}
