package com.flowmind.workflow.service;

import com.flowmind.workflow.entity.WorkflowActionLog;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;

import java.util.List;

/**
 * 工作流动作日志服务。
 */
public interface WorkflowActionLogService {

    /**
     * 记录实例维度动作。
     */
    void recordInstanceAction(
            WorkflowInstance instance,
            String action,
            String actor,
            String comment
    );

    /**
     * 记录任务维度动作。
     */
    void recordTaskAction(
            WorkflowTask task,
            WorkflowInstance instance,
            String action,
            String actor,
            String comment
    );

    /**
     * 查询某个实例的动作日志。
     */
    List<WorkflowActionLog> listByInstance(Long instanceId);
}
