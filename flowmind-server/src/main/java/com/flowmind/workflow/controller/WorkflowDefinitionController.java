package com.flowmind.workflow.controller;

import com.flowmind.workflow.dto.StartWorkflowRequest;
import com.flowmind.workflow.dto.WorkflowDefinitionRequest;
import com.flowmind.workflow.dto.WorkflowPreCheckResult;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowVersion;
import com.flowmind.workflow.service.WorkflowDefinitionService;
import com.flowmind.workflow.service.WorkflowInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流定义接口。
 *
 * 既提供流程模板管理，
 * 也提供基于某个模板启动流程实例的入口。
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService definitionService;

    private final WorkflowInstanceService instanceService;

    /**
     * 管理员创建流程定义
     * @param request
     * @return
     */
    @PostMapping
    public WorkflowDefinition create(
            @Valid @RequestBody WorkflowDefinitionRequest request
    ) {
        return definitionService.create(request);
    }

    @GetMapping
    public List<WorkflowDefinition> list(
            @RequestParam(required = false) Boolean enabled
    ) {
        if (Boolean.TRUE.equals(enabled)) {
            return definitionService.listEnabled();
        }
        return definitionService.list();
    }

    /**
     * 分页查询流程定义。
     */
    @GetMapping("/page")
    public Page<WorkflowDefinition> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return definitionService.queryPage(
                name, code, status, category,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
    }

    @GetMapping("/{id}")
    public WorkflowDefinition get(
            @PathVariable Long id
    ) {
        return definitionService.get(id);
    }

    /**
     * 更新流程定义。
     */
    @PutMapping("/{id}")
    public WorkflowDefinition update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowDefinitionRequest request
    ) {
        return definitionService.update(id, request);
    }

    /**
     * 发布前校验流程图。
     *
     * 前端会把当前画布草稿传入 request；
     * 如果 request 为空，则校验数据库中已保存的流程定义。
     */
    @PostMapping("/{id}/pre-check")
    public WorkflowPreCheckResult preCheck(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowDefinitionRequest request
    ) {
        return definitionService.preCheck(id, request);
    }

    /**
     * 发布流程定义。
     */
    @PostMapping("/{id}/publish")
    public WorkflowDefinition publish(@PathVariable Long id) {
        return definitionService.publish(id);
    }

    /**
     * 查询流程发布版本列表。
     */
    @GetMapping("/{id}/versions")
    public List<WorkflowVersion> listVersions(@PathVariable Long id) {
        return definitionService.listVersions(id);
    }

    /**
     * 启用流程定义。
     */
    @PostMapping("/{id}/enable")
    public WorkflowDefinition enable(@PathVariable Long id) {
        return definitionService.enable(id);
    }

    /**
     * 停用流程定义。
     */
    @PostMapping("/{id}/disable")
    public WorkflowDefinition disable(@PathVariable Long id) {
        return definitionService.disable(id);
    }

    /**
     * 基于某个流程定义启动一个流程实例。
     *
     * 示例：
     * POST /api/workflows/1/start
     */
    @PostMapping("/{id}/start")
    public WorkflowInstance start(
            @PathVariable Long id,
            @Valid @RequestBody StartWorkflowRequest request
    ) {
        return instanceService.start(id, request);
    }
}
