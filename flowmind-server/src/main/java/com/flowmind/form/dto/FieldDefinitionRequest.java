package com.flowmind.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字段定义保存请求。
 */
@Data
public class FieldDefinitionRequest {

    @NotBlank(message = "字段Key不能为空")
    @Size(max = 100, message = "字段Key长度不能超过100个字符")
    private String fieldKey;

    @NotBlank(message = "字段名称不能为空")
    @Size(max = 200, message = "字段名称长度不能超过200个字符")
    private String fieldName;

    @Size(max = 50, message = "字段类型长度不能超过50个字符")
    private String fieldType;

    @Size(max = 50, message = "来源类型长度不能超过50个字符")
    private String sourceType;

    @Size(max = 100, message = "来源表名长度不能超过100个字符")
    private String sourceTable;

    @Size(max = 100, message = "来源列名长度不能超过100个字符")
    private String sourceColumn;

    private String defaultValue;

    private String optionsJson;

    private String validationJson;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    private String status;
}