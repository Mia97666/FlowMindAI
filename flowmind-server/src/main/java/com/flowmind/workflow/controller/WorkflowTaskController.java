package com.flowmind.workflow.controller;

import com.flowmind.workflow.dto.CompleteTaskRequest;
import com.flowmind.workflow.entity.WorkflowTask;
import com.flowmind.workflow.service.WorkflowTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流任务接口。
 *
 * 提供：
 * 1. 查询待办任务
 * 2. 查询流程实例任务
 * 3. 完成审批任务
 */
@RestController
@RequestMapping("/api/workflow-tasks")
@RequiredArgsConstructor
public class WorkflowTaskController {

    private final WorkflowTaskService taskService;

    /**
     * 查询某个审批人的待办任务。
     *
     * 示例：
     * GET /api/workflow-tasks/todo?assignee=lisi
     */
    @GetMapping("/todo")
    public List<WorkflowTask> todo(@RequestParam String assignee) {
        return taskService.todo(assignee);
    }

    /**
     * 查询某个审批人的已处理任务。
     *
     * 示例：
     * GET /api/workflow-tasks/done?assignee=lisi
     */
    @GetMapping("/done")
    public List<WorkflowTask> done(@RequestParam String assignee) {
        return taskService.done(assignee);
    }

    /**
     * 分页查询待办任务。
     */
    @GetMapping("/todo/page")
    public Page<WorkflowTask> todoPage(
            @RequestParam String assignee,
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return taskService.queryTodo(
                assignee,
                nodeName,
                riskLevel,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 分页查询已处理任务。
     */
    @GetMapping("/done/page")
    public Page<WorkflowTask> donePage(
            @RequestParam String assignee,
            @RequestParam(required = false) String nodeName,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return taskService.queryDone(
                assignee,
                nodeName,
                riskLevel,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt", "createdAt"))
        );
    }

    /**
     * 查询某个流程实例下的全部任务。
     */
    @GetMapping("/instance/{instanceId}")
    public List<WorkflowTask> listByInstance(@PathVariable Long instanceId) {
        return taskService.listByInstance(instanceId);
    }

    /**
     * 完成审批任务。
     */
    @PostMapping("/{taskId}/complete")
    public WorkflowTask complete(
            @PathVariable Long taskId,
            @Valid @RequestBody CompleteTaskRequest request
    ) {
        return taskService.complete(taskId, request);
    }
}
