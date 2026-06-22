package com.flowmind.form.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库列元数据响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseColumnResponse {

    /**
     * 列名。
     */
    private String columnName;

    /**
     * JDBC 类型编码。
     */
    private Integer dataType;

    /**
     * 数据库原始类型名称。
     */
    private String typeName;

    /**
     * 推荐字段 Key。
     */
    private String suggestedFieldKey;

    /**
     * 推荐字段名称。
     */
    private String suggestedFieldName;

    /**
     * 推荐字段类型。
     */
    private String suggestedFieldType;

    /**
     * 列备注。
     */
    private String remarks;
}
