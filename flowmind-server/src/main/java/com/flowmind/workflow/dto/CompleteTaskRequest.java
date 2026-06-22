package com.flowmind.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 完成审批任务请求。
 */
@Data
public class CompleteTaskRequest {

    /**
     * 审批动作。
     *
     * APPROVED：通过
     * REJECTED：拒绝
     */
    @NotBlank(message = "审批动作不能为空")
    private String action;

    /**
     * 审批意见。
     */
    @Size(max = 500, message = "审批意见长度不能超过500个字符")
    private String comment;

    /**
     * 转办目标。
     *
     * 当前接口先预留，后续 TRANSFER 动作可直接使用。
     */
    @Size(max = 50, message = "转办目标长度不能超过50个字符")
    private String targetAssignee;

    /**
     * 审批表单业务数据。
     *
     * 审批人在任务页修改字段后，前端随审批动作一并提交，
     * 后端会覆盖保存到流程实例 businessDataJson 中。
     */
    private Map<String, Object> businessData;
}