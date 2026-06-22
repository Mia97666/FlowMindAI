package com.flowmind.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流定义。
 *
 * 管理员设计的流程模板。
 *
 * 示例：
 * 采购审批流程
 * 报销审批流程
 * 加班审批流程
 */
@Data
@Entity
@Table(name = "workflow_definition")
public class WorkflowDefinition {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程编码
     *
     * PURCHASE_APPROVAL
     */
    @Column(nullable = false)
    private String code;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 动态表单定义JSON。
     *
     * 前端表单设计器保存字段数组，
     * 用户发起审批时按该配置渲染输入项。
     */
    @Column(columnDefinition = "TEXT")
    private String formJson;

    /**
     * 流程定义JSON
     *
     * 前端拖拽后保存
     */
    @Column(columnDefinition = "TEXT")
    private String definitionJson;

    /**
     * BPMN XML 快照。
     *
     * 当前引擎直接执行 definitionJson，
     * 但保留 bpmnXml 便于未来接入 Flowable/Camunda 等标准 BPMN 引擎。
     */
    @Column(columnDefinition = "TEXT")
    private String bpmnXml;

    /**
     * 流程版本。
     *
     * 已发起的实例会冗余保存该版本，避免后续流程定义变更影响历史实例。
     */
    private Integer version;

    /**
     * 当前最新发布版本 ID。
     *
     * 流程实例启动时会绑定该版本，保证历史流程按发布快照执行。
     */
    private Long publishedVersionId;

    /**
     * 流程状态。
     *
     * DRAFT：草稿；PUBLISHED：已发布；DISABLED：已停用。
     */
    private String status;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 发布时间。
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
