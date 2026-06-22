package com.flowmind.audit.controller;

import com.flowmind.audit.entity.AiAuditLog;
import com.flowmind.audit.service.AiAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 审计日志接口。
 */
@RestController
@RequestMapping("/api/ai/audit-logs")
@RequiredArgsConstructor
public class AiAuditController {

    private final AiAuditService aiAuditService;

    /**
     * 查询某个流程实例下的 AI 节点执行日志。
     */
    @GetMapping("/instance/{instanceId}")
    public List<AiAuditLog> listByInstance(@PathVariable Long instanceId) {
        return aiAuditService.listByInstance(instanceId);
    }
}
