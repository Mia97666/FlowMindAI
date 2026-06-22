package com.flowmind.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.audit.entity.AiAuditLog;
import com.flowmind.audit.repository.AiAuditLogRepository;
import com.flowmind.workflow.dto.AiRiskCheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 审计服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiAuditServiceImpl implements AiAuditService {

    private final AiAuditLogRepository repository;

    private final ObjectMapper objectMapper;

    @Override
    public AiAuditLog record(
            Long instanceId,
            String workflowCode,
            String nodeId,
            AiRiskCheckResult result
    ) {
        AiAuditLog log = new AiAuditLog();
        log.setInstanceId(instanceId);
        log.setWorkflowCode(workflowCode);
        log.setNodeId(nodeId);
        log.setRiskScore(result.getRiskScore());
        log.setRiskLevel(result.getRiskLevel());
        log.setDecision(result.getDecision());
        log.setRiskReason(result.getRiskReason());
        log.setSuggestion(result.getSuggestion());
        log.setPrompt(result.getPrompt());
        log.setRagContext(result.getRagContext());
        log.setModelName(result.getModelName());
        log.setRawResponse(result.getRawResponse());
        log.setDurationMs(result.getDurationMs());
        log.setRagSourcesJson(toJson(result.getSources()));
        log.setCreatedAt(LocalDateTime.now());
        return repository.save(log);
    }

    @Override
    public List<AiAuditLog> listByInstance(Long instanceId) {
        return repository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
    }

    /**
     * 审计表保存的是只读快照，因此这里把来源列表序列化为 JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
