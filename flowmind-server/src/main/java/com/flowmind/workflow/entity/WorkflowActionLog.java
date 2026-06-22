package com.flowmind.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流动作日志。
 *
 * 该表记录实例从发起、进入节点、创建任务、审批处理到结束的关键动作，
 * 用于实例追踪、审计回放和问题排查。
 */
@Data
@Entity
@Table(name = "fm_workflow_action_log")
public class WorkflowActionLog {

    /**
     * 主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程实例 ID。
     */
    @Column(nullable = false)
    private Long instanceId;

    /**
     * 审批任务 ID，非任务动作可为空。
     */
    private Long taskId;

    /**
     * 流程定义 ID。
     */
    private Long definitionId;

    /**
     * 工作流发布版本 ID。
     */
    private Long definitionVersionId;

    /**
     * 节点 ID。
     */
    private String nodeId;

    /**
     * 节点名称。
     */
    private String nodeName;

    /**
     * 操作人。
     *
     * 系统自动动作使用 SYSTEM，用户审批动作使用用户名。
     */
    @Column(length = 128)
    private String actor;

    /**
     * 动作类型。
     *
     * START、ENTER_NODE、CREATE_TASK、APPROVED、REJECTED、TRANSFER、COMPLETE 等。
     */
    @Column(nullable = false, length = 64)
    private String action;

    /**
     * 动作结果状态。
     */
    @Column(length = 64)
    private String resultStatus;

    /**
     * 备注或审批意见。
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * 动作发生时的业务数据快照。
     */
    @Column(columnDefinition = "TEXT")
    private String businessDataJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
