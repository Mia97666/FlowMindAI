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
 * 工作流发布版本。
 *
 * WorkflowDefinition 保存管理员当前可编辑的流程定义，
 * WorkflowVersion 保存每次发布时的不可变快照。流程实例启动后绑定该快照，
 * 因此后续管理员调整节点、连线、表单绑定时，不会污染历史实例和历史任务。
 */
@Data
@Entity
@Table(name = "fm_workflow_version")
public class WorkflowVersion {

    /**
     * 主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对应的工作流定义 ID。
     */
    @Column(nullable = false)
    private Long definitionId;

    /**
     * 流程编码快照。
     */
    @Column(nullable = false, length = 128)
    private String code;

    /**
     * 流程名称快照。
     */
    @Column(length = 128)
    private String name;

    /**
     * 流程描述快照。
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 发布版本号，从 1 开始递增。
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * 流程内置表单 JSON 快照，兼容早期设计。
     */
    @Column(columnDefinition = "TEXT")
    private String formJson;

    /**
     * 流程图 JSON 快照。
     *
     * 发布时会把节点绑定的表单版本 ID 固化进节点配置。
     */
    @Column(columnDefinition = "TEXT")
    private String definitionJson;

    /**
     * BPMN XML 快照。
     */
    @Column(columnDefinition = "TEXT")
    private String bpmnXml;

    /**
     * 版本状态。
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 发布时间。
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
