package com.flowmind.form.dto;

import lombok.Data;

/**
 * 单个数据库列导入项。
 */
@Data
public class DatabaseFieldImportItem {

    private String sourceColumn;

    private String fieldKey;

    private String fieldName;

    private String fieldType;
}
