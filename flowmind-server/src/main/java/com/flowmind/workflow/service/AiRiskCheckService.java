package com.flowmind.workflow.service;

import com.flowmind.workflow.dto.AiRiskCheckResult;

import java.util.Map;

/**
 * AI 风险检测服务。
 *
 * 作用：
 * 根据用户提交的业务数据，结合企业知识库制度，
 * 判断当前审批申请是否存在风险。
 */
public interface AiRiskCheckService {

    /**
     * 执行 AI 风险检测。
     *
     * @param workflowCode 流程编码，例如 PURCHASE_APPROVAL
     * @param businessData 用户提交的业务数据
     * @return AI 风险检测结果
     */
    AiRiskCheckResult check(
            String workflowCode,
            Map<String, Object> businessData
    );
}
