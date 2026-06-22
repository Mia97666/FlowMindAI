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
 * 流程表单定义。
 *
 * 表单定义保存的是“表单模板”，工作流节点后续通过 formCode/formId 绑定该模板，
 * 审批发起、审批处理、审批查看都按同一份 schema 渲染，避免不同页面各自写死字段。
 */
@Data
@Entity
@Table(name = "fm_form_definition")
public class FormDefinition {

    /**
     * 主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 表单编码，业务唯一。
     */
    @Column(nullable = false, unique = true, length = 128)
    private String formCode;

    /**
     * 表单名称。
     */
    @Column(nullable = false, length = 128)
    private String formName;

    /**
     * 表单分类。
     *
     * 例如采购、报销、人事、通用。
     */
    @Column(length = 64)
    private String category;

    /**
     * 表单版本。
     */
    private Integer version;

    /**
     * 当前最新发布版本 ID。
     *
     * 草稿继续保存在本表，运行态实例优先使用该 ID 对应的 FormVersion。
     */
    private Long publishedVersionId;

    /**
     * 表单状态。
     *
     * DRAFT：草稿；PUBLISHED：已发布；DISABLED：停用。
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 表单布局和控件配置 JSON。
     *
     * 结构示例：
     * {"fields":[{"id":"amount","fieldKey":"amount","label":"采购金额","componentType":"NUMBER"}]}
     */
    @Column(columnDefinition = "TEXT")
    private String schemaJson;

    /**
     * 表单说明。
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

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
