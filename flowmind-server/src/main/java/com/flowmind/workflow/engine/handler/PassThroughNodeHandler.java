package com.flowmind.workflow.engine.handler;

import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 普通透传节点处理器。
 *
 * START 和 FORM 节点本身不产生后端动作，
 * 只负责在流程图中表达“从哪里开始”和“用户提交表单”的语义。
 */
@Component
public class PassThroughNodeHandler implements NodeHandler {

    private static final Set<String> SUPPORT_TYPES = Set.of("START", "FORM");

    @Override
    public boolean support(String nodeType) {
        return SUPPORT_TYPES.contains(nodeType.toUpperCase());
    }

    @Override
    public NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    ) {
        return NodeExecutionResult.continueNext();
    }
}
