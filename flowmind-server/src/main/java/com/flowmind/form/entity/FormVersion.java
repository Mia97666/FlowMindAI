package com.flowmind.form.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表单发布版本。
 *
 * FormDefinition 是可编辑的表单草稿/当前定义，FormVersion 是发布时生成的不可变快照。
 * 流程实例和审批任务应优先绑定 FormVersion，避免管理员后续修改表单后影响历史审批回放。
 */
@Data
@Entity
@Table(name = "fm_form_version")
public class FormVersion {

    /**
     * 主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对应的表单定义 ID。
     */
    @Column(nullable = false)
    private Long formDefinitionId;

    /**
     * 表单编码快照。
     */
    @Column(nullable = false, length = 128)
    private String formCode;

    /**
     * 表单名称快照。
     */
    @Column(nullable = false, length = 128)
    private String formName;

    /**
     * 表单分类快照。
     */
    @Column(length = 64)
    private String category;

    /**
     * 发布版本号，从 1 开始递增。
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * 版本状态。
     *
     * 当前先固定为 PUBLISHED，后续如需要可扩展为 ARCHIVED。
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 发布时的完整表单 Schema。
     */
    @Column(columnDefinition = "TEXT")
    private String schemaJson;

    /**
     * 表单说明快照。
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 发布时间。
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
