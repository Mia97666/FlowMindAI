package com.flowmind.audit.repository;

import com.flowmind.audit.entity.AiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AI 审计日志数据访问接口。
 */
public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, Long> {

    /**
     * 查询某个流程实例的 AI 审计记录。
     */
    List<AiAuditLog> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);
}
