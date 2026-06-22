package com.flowmind.workflow.controller;

import com.flowmind.workflow.entity.WorkflowActionLog;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.service.WorkflowActionLogService;
import com.flowmind.workflow.service.WorkflowInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流实例接口。
 *
 * 用于查询已经发起的审批流程。
 */
@RestController
@RequestMapping("/api/workflow-instances")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceService instanceService;

    private final WorkflowActionLogService actionLogService;

    /**
     * 查询全部流程实例。
     */
    @GetMapping
    public List<WorkflowInstance> list(
            @RequestParam(required = false) String starter
    ) {
        return instanceService.list(starter);
    }

    /**
     * 分页查询流程实例。
     */
    @GetMapping("/page")
    public Page<WorkflowInstance> page(
            @RequestParam(required = false) String starter,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long definitionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return instanceService.queryPage(
                starter,
                title,
                definitionId,
                status,
                riskLevel,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 查询单个流程实例。
     */
    @GetMapping("/{id}")
    public WorkflowInstance get(@PathVariable Long id) {
        return instanceService.get(id);
    }

    /**
     * 查询当前用户的申请列表（我的申请）。
     */
    @GetMapping("/my")
    public Page<WorkflowInstance> myApplications(
            @RequestParam String starter,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return instanceService.queryPage(
                starter,
                title,
                null,
                status,
                null,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 查询流程实例动作日志。
     */
    @GetMapping("/{id}/logs")
    public List<WorkflowActionLog> logs(@PathVariable Long id) {
        instanceService.get(id);
        return actionLogService.listByInstance(id);
    }
}
