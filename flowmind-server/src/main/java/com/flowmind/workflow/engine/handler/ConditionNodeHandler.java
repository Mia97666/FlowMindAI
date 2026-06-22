package com.flowmind.workflow.engine.handler;

import com.flowmind.workflow.engine.model.NodeExecutionResult;
import com.flowmind.workflow.engine.model.WorkflowEdge;
import com.flowmind.workflow.engine.model.WorkflowExecutionContext;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.engine.support.ConditionEvaluator;
import com.flowmind.workflow.service.WorkflowActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 条件路由节点处理器。
 *
 * 支持 CONDITION 和 EXCLUSIVE_GATEWAY，两者都按“第一条命中条件的出边”流转。
 */
@Component
@RequiredArgsConstructor
public class ConditionNodeHandler implements NodeHandler {

    private final ConditionEvaluator conditionEvaluator;

    private final WorkflowActionLogService actionLogService;

    @Override
    public boolean support(String nodeType) {
        return "CONDITION".equalsIgnoreCase(nodeType)
                || "EXCLUSIVE_GATEWAY".equalsIgnoreCase(nodeType);
    }

    @Override
    public NodeExecutionResult handle(
            WorkflowExecutionContext context,
            WorkflowNode node
    ) {
        List<WorkflowEdge> outgoingEdges = context.getGraph().getEdges()
                .stream()
                .filter(edge -> node.getId().equals(edge.getSource()))
                .toList();

        WorkflowEdge defaultEdge = null;
        for (WorkflowEdge edge : outgoingEdges) {
            if (conditionEvaluator.isDefaultCondition(edge.getCondition())) {
                defaultEdge = edge;
                continue;
            }

            if (conditionEvaluator.evaluate(
                    edge.getCondition(),
                    context.getInstance(),
                    context.getBusinessData()
            )) {
                recordRouteMatched(context, node, edge);
                return NodeExecutionResult.continueTo(edge.getTarget());
            }
        }

        if (defaultEdge == null) {
            actionLogService.recordInstanceAction(
                    context.getInstance(),
                    "ROUTE_NOT_MATCHED",
                    "SYSTEM",
                    "条件节点【" + node.getName() + "】未命中任何分支，交由执行器选择后续节点。"
            );
            return NodeExecutionResult.continueNext();
        }

        recordRouteMatched(context, node, defaultEdge);
        return NodeExecutionResult.continueTo(defaultEdge.getTarget());
    }

    private void recordRouteMatched(
            WorkflowExecutionContext context,
            WorkflowNode node,
            WorkflowEdge edge
    ) {
        actionLogService.recordInstanceAction(
                context.getInstance(),
                "ROUTE_MATCHED",
                "SYSTEM",
                "条件节点【" + node.getName() + "】命中分支【"
                        + nullToText(edge.getLabel())
                        + "】，条件：" + nullToText(edge.getCondition())
                        + "，目标节点：" + edge.getTarget()
        );
    }

    private String nullToText(String value) {
        return value == null || value.isBlank() ? "默认" : value;
    }
}
