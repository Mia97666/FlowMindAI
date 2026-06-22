package com.flowmind.form.controller;

import com.flowmind.form.dto.FormDefinitionRequest;
import com.flowmind.form.entity.FormDefinition;
import com.flowmind.form.entity.FormVersion;
import com.flowmind.form.service.FormDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 表单定义管理接口。
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class FormDefinitionController {

    private final FormDefinitionService formDefinitionService;

    @GetMapping
    public List<FormDefinition> list(
            @RequestParam(required = false) String formCode,
            @RequestParam(required = false) String formName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean enabled
    ) {
        return formDefinitionService.list(formCode, formName, category, status, enabled);
    }

    /**
     * 分页查询表单定义。
     */
    @GetMapping("/page")
    public Page<FormDefinition> page(
            @RequestParam(required = false) String formCode,
            @RequestParam(required = false) String formName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return formDefinitionService.queryPage(
                formCode, formName, category, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
    }

    @GetMapping("/{id}")
    public FormDefinition get(@PathVariable Long id) {
        return formDefinitionService.get(id);
    }

    @PostMapping
    public FormDefinition create(@Valid @RequestBody FormDefinitionRequest request) {
        return formDefinitionService.create(request);
    }

    @PutMapping("/{id}")
    public FormDefinition update(
            @PathVariable Long id,
            @Valid @RequestBody FormDefinitionRequest request
    ) {
        return formDefinitionService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public FormDefinition publish(@PathVariable Long id) {
        return formDefinitionService.publish(id);
    }

    @GetMapping("/{id}/versions")
    public List<FormVersion> listVersions(@PathVariable Long id) {
        return formDefinitionService.listVersions(id);
    }

    @PostMapping("/{id}/enable")
    public FormDefinition enable(@PathVariable Long id) {
        return formDefinitionService.enable(id);
    }

    @PostMapping("/{id}/disable")
    public FormDefinition disable(@PathVariable Long id) {
        return formDefinitionService.disable(id);
    }
}
