package com.flowmind.workflow.repository;

import com.flowmind.workflow.entity.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 工作流任务 Repository。
 */
public interface WorkflowTaskRepository
        extends JpaRepository<WorkflowTask, Long>, JpaSpecificationExecutor<WorkflowTask> {

    /**
     * 根据审批人查询待办任务。
     */
    List<WorkflowTask> findByAssigneeAndStatusOrderByCreatedAtDesc(
            String assignee,
            String status
    );

    /**
     * 根据审批人查询已处理任务。
     */
    List<WorkflowTask> findByAssigneeAndStatusInOrderByCompletedAtDescCreatedAtDesc(
            String assignee,
            Collection<String> statuses
    );

    /**
     * 查询某个流程实例下的所有任务。
     */
    List<WorkflowTask> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);

    /**
     * 查询某个实例某个节点是否已有待办。
     *
     * 流程执行器可能因为页面重试或服务重启被再次触发，
     * 这里用于避免重复创建同一个审批节点的待办任务。
     */
    Optional<WorkflowTask> findByInstanceIdAndNodeIdAndStatus(
            Long instanceId,
            String nodeId,
            String status
    );
}
