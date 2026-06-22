package com.flowmind.workflow.engine;

import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;

/**
 * 动态工作流执行服务。
 */
public interface WorkflowExecutionService {

    /**
     * 从开始节点启动流程。
     */
    WorkflowInstance start(WorkflowInstance instance);

    /**
     * 某个人工任务审批通过后，从该节点继续向后执行。
     */
    WorkflowInstance continueAfterTask(WorkflowTask task);

    /**
     * 某个人工任务拒绝后，终止流程实例。
     */
    WorkflowInstance rejectByTask(WorkflowTask task);
}
