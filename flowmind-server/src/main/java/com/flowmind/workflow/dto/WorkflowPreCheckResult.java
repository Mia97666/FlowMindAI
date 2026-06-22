package com.flowmind.workflow.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流发布前校验结果。
 *
 * errors 会阻断发布，warnings 只提示管理员关注。
 */
@Data
public class WorkflowPreCheckResult {

    /**
     * 是否通过发布前校验。
     */
    private boolean passed;

    /**
     * 阻断发布的问题。
     */
    private List<String> errors = new ArrayList<>();

    /**
     * 不阻断发布的提醒。
     */
    private List<String> warnings = new ArrayList<>();

    public static WorkflowPreCheckResult of(
            List<String> errors,
            List<String> warnings
    ) {
        WorkflowPreCheckResult result = new WorkflowPreCheckResult();
        result.setErrors(errors == null ? new ArrayList<>() : errors);
        result.setWarnings(warnings == null ? new ArrayList<>() : warnings);
        result.setPassed(result.getErrors().isEmpty());
        return result;
    }
}
