package com.flowmind.workflow.service;

import com.flowmind.workflow.dto.CompleteTaskRequest;
import com.flowmind.workflow.entity.WorkflowTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 工作流任务服务。
 */
public interface WorkflowTaskService {

    /**
     * 创建人工审批任务。
     */
    WorkflowTask createApprovalTask(
            Long instanceId,
            Long definitionId,
            String nodeId,
            String nodeName,
            String assignee
    );

    /**
     * 查询某人的待办任务。
     */
    List<WorkflowTask> todo(String assignee);

    /**
     * 查询某人的已处理任务。
     */
    List<WorkflowTask> done(String assignee);

    /**
     * 分页查询待办任务。
     */
    Page<WorkflowTask> queryTodo(
            String assignee,
            String nodeName,
            String riskLevel,
            Pageable pageable
    );

    /**
     * 分页查询已处理任务。
     */
    Page<WorkflowTask> queryDone(
            String assignee,
            String nodeName,
            String riskLevel,
            String status,
            Pageable pageable
    );

    /**
     * 查询某个流程实例的任务列表。
     */
    List<WorkflowTask> listByInstance(Long instanceId);

    /**
     * 完成审批任务。
     */
    WorkflowTask complete(Long taskId, CompleteTaskRequest request);
}
