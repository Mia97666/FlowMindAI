package com.flowmind.workflow.repository;

import com.flowmind.workflow.entity.WorkflowActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 工作流动作日志数据访问。
 */
public interface WorkflowActionLogRepository extends JpaRepository<WorkflowActionLog, Long> {

    /**
     * 查询某个流程实例的全部动作日志。
     */
    List<WorkflowActionLog> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);
}
