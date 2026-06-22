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
 * 流程表单字段定义。
 *
 * 字段定义是表单设计器的基础数据源：
 * 1. 自定义字段由管理员直接创建。
 * 2. 数据库字段通过元数据导入，保留来源表和来源列，便于后续和业务数据打通。
 */
@Data
@Entity
@Table(name = "fm_field_definition")
public class FieldDefinition {

    /**
     * 主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 字段唯一标识。
     *
     * 前端表单控件绑定、流程实例 businessData 入参都使用该 Key。
     */
    @Column(nullable = false, unique = true, length = 128)
    private String fieldKey;

    /**
     * 字段展示名称。
     */
    @Column(nullable = false, length = 128)
    private String fieldName;

    /**
     * 字段类型。
     *
     * TEXT、TEXTAREA、NUMBER、AMOUNT、DATE、DATETIME、SELECT、BOOLEAN。
     */
    @Column(nullable = false, length = 32)
    private String fieldType;

    /**
     * 字段来源。
     *
     * CUSTOM：自定义字段；DB_COLUMN：数据库字段；SYSTEM：系统内置字段。
     */
    @Column(nullable = false, length = 32)
    private String sourceType;

    /**
     * 数据库来源表，仅 DB_COLUMN 字段需要填写。
     */
    @Column(length = 128)
    private String sourceTable;

    /**
     * 数据库来源列，仅 DB_COLUMN 字段需要填写。
     */
    @Column(length = 128)
    private String sourceColumn;

    /**
     * 默认值。
     */
    @Column(length = 512)
    private String defaultValue;

    /**
     * 选项配置 JSON。
     *
     * 下拉、多选、单选等字段使用该字段保存候选项。
     */
    @Column(columnDefinition = "TEXT")
    private String optionsJson;

    /**
     * 校验配置 JSON。
     *
     * 例如必填、长度、正则表达式、数值范围等规则。
     */
    @Column(columnDefinition = "TEXT")
    private String validationJson;

    /**
     * 字段说明。
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 状态。
     *
     * ENABLED：启用；DISABLED：停用。
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
