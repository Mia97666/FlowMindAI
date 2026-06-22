package com.flowmind.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工作流请求
 */
@Data
public class WorkflowDefinitionRequest {

    /**
     * 流程编码
     */
    @NotBlank(message = "流程编码不能为空")
    @Size(max = 100, message = "流程编码长度不能超过100个字符")
    private String code;

    /**
     * 流程名称
     */
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 200, message = "流程名称长度不能超过200个字符")
    private String name;

    /**
     * 描述
     */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /**
     * 动态表单定义JSON。
     */
    private String formJson;

    /**
     * 流程JSON
     */
    private String definitionJson;

    /**
     * BPMN XML 快照。
     */
    private String bpmnXml;

    /**
     * 流程版本。
     */
    private Integer version;

    /**
     * 流程状态，DRAFT / PUBLISHED / DISABLED。
     */
    private String status;
}