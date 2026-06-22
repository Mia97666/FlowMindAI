package com.flowmind.workflow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.common.exception.BadRequestException;
import com.flowmind.common.exception.ConflictException;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.workflow.dto.CompleteTaskRequest;
import com.flowmind.workflow.engine.WorkflowExecutionService;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;
import com.flowmind.workflow.repository.WorkflowInstanceRepository;
import com.flowmind.workflow.repository.WorkflowTaskRepository;
import com.flowmind.workflow.service.WorkflowActionLogService;
import com.flowmind.workflow.service.WorkflowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流任务服务实现。
 *
 * 当前阶段：
 * 只处理人工审批任务的创建、查询和完成。
 *
 * 后续会扩展：
 * 1. 完成任务后自动流转到下一个节点
 * 2. 审批拒绝后结束流程
 * 3. 审批通过后继续执行流程
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    private final WorkflowTaskRepository taskRepository;

    private final WorkflowInstanceRepository instanceRepository;

    private final WorkflowExecutionService workflowExecutionService;

    private final ObjectMapper objectMapper;

    private final WorkflowActionLogService actionLogService;

    @Override
    public WorkflowTask createApprovalTask(
            Long instanceId,
            Long definitionId,
            String nodeId,
            String nodeName,
            String assignee
    ) {
        WorkflowTask task = new WorkflowTask();
        WorkflowInstance instance = instanceRepository.findById(instanceId).orElse(null);

        task.setInstanceId(instanceId);
        task.setDefinitionId(definitionId);
        task.setDefinitionVersionId(instance == null ? null : instance.getDefinitionVersionId());
        task.setNodeId(nodeId);
        task.setNodeName(nodeName);
        task.setAssignee(assignee);

        /**
         * 新建任务默认是待处理状态。
         */
        task.setStatus("PENDING");

        task.setCreatedAt(LocalDateTime.now());

        WorkflowTask savedTask = taskRepository.save(task);
        if (instance != null) {
            actionLogService.recordTaskAction(
                    savedTask,
                    instance,
                    "CREATE_TASK",
                    "SYSTEM",
                    "创建待办任务，处理人：" + assignee
            );
        }
        return savedTask;
    }

    @Override
    public List<WorkflowTask> todo(String assignee) {
        return taskRepository.findByAssigneeAndStatusOrderByCreatedAtDesc(
                assignee,
                "PENDING"
        );
    }

    @Override
    public List<WorkflowTask> done(String assignee) {
        return taskRepository.findByAssigneeAndStatusInOrderByCompletedAtDescCreatedAtDesc(
                assignee,
                List.of("APPROVED", "REJECTED")
        );
    }

    @Override
    public Page<WorkflowTask> queryTodo(
            String assignee,
            String nodeName,
            String riskLevel,
            Pageable pageable
    ) {
        return taskRepository.findAll(taskSpecification(
                assignee,
                nodeName,
                riskLevel,
                "PENDING"
        ), pageable);
    }

    @Override
    public Page<WorkflowTask> queryDone(
            String assignee,
            String nodeName,
            String riskLevel,
            String status,
            Pageable pageable
    ) {
        Specification<WorkflowTask> specification = taskSpecification(
                assignee,
                nodeName,
                riskLevel,
                status
        ).and((root, query, criteriaBuilder) -> {
            if (StringUtils.hasText(status)) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in("APPROVED", "REJECTED");
        });
        return taskRepository.findAll(specification, pageable);
    }

    @Override
    public List<WorkflowTask> listByInstance(Long instanceId) {
        return taskRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);
    }

    @Override
    public WorkflowTask complete(Long taskId, CompleteTaskRequest request) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("任务", taskId));

        if (!"PENDING".equals(task.getStatus())) {
            throw new ConflictException("任务已处理，不能重复审批");
        }

        if (!"APPROVED".equals(request.getAction())
                && !"REJECTED".equals(request.getAction())
                && !"TRANSFER".equals(request.getAction())) {
            throw new BadRequestException("审批动作不合法：" + request.getAction());
        }

        if ("TRANSFER".equals(request.getAction())) {
            if (request.getTargetAssignee() == null || request.getTargetAssignee().isBlank()) {
                throw new BadRequestException("转办目标不能为空");
            }
            String originalAssignee = task.getAssignee();
            task.setAssignee(request.getTargetAssignee());
            task.setComment(request.getComment());
            WorkflowTask savedTask = taskRepository.save(task);
            WorkflowInstance instance = instanceRepository.findById(task.getInstanceId())
                    .orElseThrow(() -> new NotFoundException("流程实例", task.getInstanceId()));
            actionLogService.recordTaskAction(
                    savedTask,
                    instance,
                    "TRANSFER",
                    originalAssignee,
                    "转办给：" + request.getTargetAssignee()
            );
            return savedTask;
        }

        updateInstanceBusinessData(task, request);

        task.setAction(request.getAction());
        task.setStatus(request.getAction());
        task.setComment(request.getComment());
        task.setCompletedAt(LocalDateTime.now());

        WorkflowTask savedTask = taskRepository.save(task);
        WorkflowInstance instance = instanceRepository.findById(savedTask.getInstanceId())
                .orElseThrow(() -> new RuntimeException("流程实例不存在：" + savedTask.getInstanceId()));
        actionLogService.recordTaskAction(
                savedTask,
                instance,
                savedTask.getStatus(),
                savedTask.getAssignee(),
                savedTask.getComment()
        );

        if ("APPROVED".equals(savedTask.getStatus())) {
            workflowExecutionService.continueAfterTask(savedTask);
        } else {
            workflowExecutionService.rejectByTask(savedTask);
        }

        return savedTask;
    }

    /**
     * 审批页提交的动态表单数据需要写回实例。
     *
     * 这样后续节点、实例详情、AI 审计都能看到最新业务数据。
     */
    private void updateInstanceBusinessData(WorkflowTask task, CompleteTaskRequest request) {
        if (request.getBusinessData() == null) {
            return;
        }
        WorkflowInstance instance = instanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new NotFoundException("流程实例", task.getInstanceId()));
        try {
            instance.setBusinessDataJson(objectMapper.writeValueAsString(request.getBusinessData()));
            instance.setUpdatedAt(LocalDateTime.now());
            instanceRepository.save(instance);
        } catch (Exception exception) {
            throw new BadRequestException("审批表单数据JSON序列化失败", exception);
        }
    }

    private Specification<WorkflowTask> taskSpecification(
            String assignee,
            String nodeName,
            String riskLevel,
            String status
    ) {
        return SpecificationBuilder.<WorkflowTask>builder()
                .equal("assignee", assignee)
                .like("nodeName", nodeName)
                .equal("riskLevel", riskLevel)
                .equal("status", status)
                .build();
    }
}
