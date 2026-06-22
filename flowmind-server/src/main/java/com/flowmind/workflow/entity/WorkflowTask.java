package com.flowmind.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流人工任务。
 *
 * 含义：
 * 流程执行到人工审批节点时，
 * 系统会创建一条待办任务。
 *
 * 示例：
 * 采购30台MacBook审批
 * ↓
 * 生成一条"直属领导审批"任务
 * ↓
 * 分配给 lisi
 */
@Data
@Entity
@Table(name = "workflow_task")
public class WorkflowTask {

    /**
     * 任务ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程实例ID。
     *
     * 对应 workflow_instance.id。
     */
    private Long instanceId;

    /**
     * 流程定义ID。
     *
     * 冗余保存，方便查询。
     */
    private Long definitionId;

    /**
     * 工作流发布版本 ID。
     *
     * 与实例保持一致，便于任务详情按历史流程快照渲染。
     */
    private Long definitionVersionId;

    /**
     * 节点ID。
     *
     * 例如：
     * manager_approve
     * finance_approve
     */
    private String nodeId;

    /**
     * 节点名称。
     *
     * 例如：
     * 直属领导审批
     * 财务审批
     */
    private String nodeName;

    /**
     * 当前任务节点绑定的表单版本 ID。
     *
     * 审批人打开任务时优先使用该版本渲染表单。
     */
    private Long bindFormVersionId;

    /**
     * 审批人。
     *
     * 当前阶段先用字符串模拟。
     * 后续可替换为 userId。
     */
    private String assignee;

    /**
     * 任务状态。
     *
     * PENDING：待处理
     * APPROVED：已通过
     * REJECTED：已拒绝
     */
    private String status;

    /**
     * 任务动作。
     *
     * 当前主要记录 APPROVED / REJECTED，
     * 后续转办、驳回等动作也可以复用该字段。
     */
    private String action;

    /**
     * 审批意见。
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * 任务创建时的 AI 风险分数快照。
     *
     * 审批人打开待办列表时可以直接看到风险等级，无需额外查询实例。
     */
    private Integer riskScore;

    /**
     * 任务创建时的 AI 风险等级快照。
     */
    private String riskLevel;

    /**
     * 乐观锁版本号。
     *
     * 防止两个审批人同时对同一任务操作，导致任务被处理两次。
     */
    @Version
    private Long version;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 完成时间。
     */
    private LocalDateTime completedAt;
}
