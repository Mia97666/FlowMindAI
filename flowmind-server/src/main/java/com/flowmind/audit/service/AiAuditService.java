package com.flowmind.audit.service;

import com.flowmind.audit.entity.AiAuditLog;
import com.flowmind.workflow.dto.AiRiskCheckResult;

import java.util.List;

/**
 * AI 审计服务接口。
 */
public interface AiAuditService {

    /**
     * 记录一次 AI 节点执行结果。
     */
    AiAuditLog record(
            Long instanceId,
            String workflowCode,
            String nodeId,
            AiRiskCheckResult result
    );

    /**
     * 查询流程实例的 AI 审计日志。
     */
    List<AiAuditLog> listByInstance(Long instanceId);
}
