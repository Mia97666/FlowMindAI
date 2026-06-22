package com.flowmind.workflow.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 节点执行结果。
 */
@Data
@AllArgsConstructor
public class NodeExecutionResult {

    /**
     * 执行状态。
     */
    private NodeExecutionStatus status;

    /**
     * 指定下一个节点。
     *
     * 条件节点会直接返回命中的目标节点；
     * 普通节点为空时由执行器根据连线自动选择。
     */
    private String nextNodeId;

    public static NodeExecutionResult continueNext() {
        return new NodeExecutionResult(NodeExecutionStatus.CONTINUE, null);
    }

    public static NodeExecutionResult continueTo(String nextNodeId) {
        return new NodeExecutionResult(NodeExecutionStatus.CONTINUE, nextNodeId);
    }

    public static NodeExecutionResult waiting() {
        return new NodeExecutionResult(NodeExecutionStatus.WAITING, null);
    }

    public static NodeExecutionResult completed() {
        return new NodeExecutionResult(NodeExecutionStatus.COMPLETED, null);
    }

    public static NodeExecutionResult rejected() {
        return new NodeExecutionResult(NodeExecutionStatus.REJECTED, null);
    }
}
