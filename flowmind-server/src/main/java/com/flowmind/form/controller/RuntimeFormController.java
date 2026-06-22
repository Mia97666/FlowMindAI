package com.flowmind.form.controller;

import com.flowmind.form.dto.RuntimeFormResponse;
import com.flowmind.form.service.RuntimeFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运行时表单接口。
 */
@RestController
@RequestMapping("/api/runtime-forms")
@RequiredArgsConstructor
public class RuntimeFormController {

    private final RuntimeFormService runtimeFormService;

    /**
     * 查询某个流程发起时应该渲染的表单。
     */
    @GetMapping("/workflows/{definitionId}/start")
    public RuntimeFormResponse resolveStartForm(@PathVariable Long definitionId) {
        return runtimeFormService.resolveStartForm(definitionId);
    }

    /**
     * 查询某个待办任务处理时应该渲染的表单。
     */
    @GetMapping("/tasks/{taskId}")
    public RuntimeFormResponse resolveTaskForm(@PathVariable Long taskId) {
        return runtimeFormService.resolveTaskForm(taskId);
    }

    /**
     * 查询流程实例查看页应该渲染的表单。
     */
    @GetMapping("/instances/{instanceId}")
    public RuntimeFormResponse resolveInstanceForm(
            @PathVariable Long instanceId,
            @RequestParam(required = false) String nodeId
    ) {
        return runtimeFormService.resolveInstanceForm(instanceId, nodeId);
    }
}
