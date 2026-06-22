package com.flowmind.workflow.engine.model;

/**
 * 节点执行状态。
 */
public enum NodeExecutionStatus {

    /**
     * 当前节点已处理，继续流转到下一个节点。
     */
    CONTINUE,

    /**
     * 当前节点创建了人工待办，需要等待审批人处理。
     */
    WAITING,

    /**
     * 流程已完成。
     */
    COMPLETED,

    /**
     * 流程被拒绝。
     */
    REJECTED
}
