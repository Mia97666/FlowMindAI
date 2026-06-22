package com.flowmind.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 表单定义保存请求。
 */
@Data
public class FormDefinitionRequest {

    @NotBlank(message = "表单编码不能为空")
    @Size(max = 100, message = "表单编码长度不能超过100个字符")


    private String formCode;

    @NotBlank(message = "表单名称不能为空")
    @Size(max = 200, message = "表单名称长度不能超过200个字符")
    private String formName;

    @Size(max = 50, message = "分类长度不能超过50个字符")
    private String category;

    private Integer version;

    private String status;

    private String schemaJson;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}