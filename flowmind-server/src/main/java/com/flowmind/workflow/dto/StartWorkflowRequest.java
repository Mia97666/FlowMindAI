package com.flowmind.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 启动流程请求。
 *
 * 用户选择某个流程模板后，提交业务数据，
 * 后端根据流程定义创建流程实例。
 */
@Data
public class StartWorkflowRequest {

    /**
     * 发起人。
     *
     * 当前阶段先让前端传。
     * 后续接入登录后，从当前登录用户中获取。
     */
    @NotBlank(message = "发起人不能为空")
    @Size(max = 50, message = "发起人长度不能超过50个字符")
    private String starter;

    /**
     * 流程标题。
     *
     * 示例：
     * 采购30台MacBook审批
     */
    @NotBlank(message = "流程标题不能为空")
    @Size(max = 200, message = "流程标题长度不能超过200个字符")
    private String title;

    /**
     * 业务数据。
     *
     * 这是动态表单提交的数据。
     * 不同流程可以有不同字段。
     *
     * 采购审批可能是：
     * amount、item、quantity、purpose
     *
     * 报销审批可能是：
     * amount、expenseType、invoiceNo、reason
     */
    private Map<String, Object> businessData;
}