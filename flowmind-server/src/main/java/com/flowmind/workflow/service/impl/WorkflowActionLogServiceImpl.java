package com.flowmind.workflow.service.impl;

import com.flowmind.workflow.entity.WorkflowActionLog;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;
import com.flowmind.workflow.repository.WorkflowActionLogRepository;
import com.flowmind.workflow.service.WorkflowActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流动作日志服务实现。
 *
 * 日志写入采用“尽量轻量”的方式：业务服务只告诉这里发生了什么，
 * 本类统一补齐实例、任务、节点、版本和业务数据快照。
 */
@Service
@RequiredArgsConstructor
public class WorkflowActionLogServiceImpl implements WorkflowActionLogService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final WorkflowActionLogRepository actionLogRepository;

    @Override
    public void recordInstanceAction(
            WorkflowInstance instance,
            String action,
            String actor,
            String comment
    ) {
        WorkflowActionLog log = baseLog(instance, null);
        log.setActor(normalizeActor(actor));
        log.setAction(action);
        log.setResultStatus(instance.getStatus());
        log.setComment(comment);
        actionLogRepository.save(log);
    }

    @Override
    public void recordTaskAction(
            WorkflowTask task,
            WorkflowInstance instance,
            String action,
            String actor,
            String comment
    ) {
        WorkflowActionLog log = baseLog(instance, task);
        log.setActor(normalizeActor(actor));
        log.setAction(action);
        log.setResultStatus(task.getStatus());
        log.setComment(comment);
        actionLogRepository.save(log);
    }

    @Override
    public List<WorkflowActionLog> listByInstance(Long instanceId) {
        return actionLogRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
    }

    private WorkflowActionLog baseLog(
            WorkflowInstance instance,
            WorkflowTask task
    ) {
        WorkflowActionLog log = new WorkflowActionLog();
        log.setInstanceId(instance.getId());
        log.setTaskId(task == null ? null : task.getId());
        log.setDefinitionId(instance.getDefinitionId());
        log.setDefinitionVersionId(instance.getDefinitionVersionId());
        log.setNodeId(task == null ? instance.getCurrentNodeId() : task.getNodeId());
        log.setNodeName(task == null ? instance.getCurrentNodeName() : task.getNodeName());
        log.setBusinessDataJson(instance.getBusinessDataJson());
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? SYSTEM_ACTOR : actor;
    }
}
