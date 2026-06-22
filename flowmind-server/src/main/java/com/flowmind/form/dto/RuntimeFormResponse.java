package com.flowmind.form.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时表单响应。
 *
 * 前端在发起审批、处理待办、查看实例时都使用该对象渲染表单，
 * 这样表单模板变更后，运行态页面不需要各自维护字段配置。
 */
@Data
public class RuntimeFormResponse {

    private Long definitionId;

    private Long definitionVersionId;

    private Integer definitionVersion;

    private Long instanceId;

    private Long taskId;

    private String nodeId;

    private String nodeName;

    private String formCode;

    private Long formVersionId;

    private Integer formVersion;

    private String formName;

    private String category;

    private String schemaJson;

    private Boolean readOnly;

    private Boolean fallback;

    private Map<String, Object> businessData = new LinkedHashMap<>();
}
