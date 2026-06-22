package com.flowmind.form.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库表元数据响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseTableResponse {

    /**
     * 表名。
     */
    private String tableName;

    /**
     * 表类型。
     */
    private String tableType;

    /**
     * 表备注。
     */
    private String remarks;
}
