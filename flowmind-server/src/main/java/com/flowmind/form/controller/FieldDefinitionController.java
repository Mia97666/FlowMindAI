package com.flowmind.form.controller;

import com.flowmind.form.dto.DatabaseColumnResponse;
import com.flowmind.form.dto.DatabaseFieldImportRequest;
import com.flowmind.form.dto.DatabaseTableResponse;
import com.flowmind.form.dto.FieldDefinitionRequest;
import com.flowmind.form.entity.FieldDefinition;
import com.flowmind.form.service.FieldDefinitionService;
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
 * 表单字段定义管理接口。
 */
@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
public class FieldDefinitionController {

    private final FieldDefinitionService fieldDefinitionService;

    /**
     * 分页能力后续接入统一分页模型；当前原型阶段先返回过滤后的完整列表。
     */
    @GetMapping
    public List<FieldDefinition> list(
            @RequestParam(required = false) String fieldKey,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String fieldType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status
    ) {
        return fieldDefinitionService.list(fieldKey, fieldName, fieldType, sourceType, status);
    }

    /**
     * 分页查询字段定义。
     */
    @GetMapping("/page")
    public Page<FieldDefinition> page(
            @RequestParam(required = false) String fieldKey,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String fieldType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return fieldDefinitionService.queryPage(
                fieldKey, fieldName, fieldType, sourceType, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
    }

    @GetMapping("/{id}")
    public FieldDefinition get(@PathVariable Long id) {
        return fieldDefinitionService.get(id);
    }

    @PostMapping
    public FieldDefinition create(@Valid @RequestBody FieldDefinitionRequest request) {
        return fieldDefinitionService.create(request);
    }

    @PutMapping("/{id}")
    public FieldDefinition update(
            @PathVariable Long id,
            @Valid @RequestBody FieldDefinitionRequest request
    ) {
        return fieldDefinitionService.update(id, request);
    }

    @PostMapping("/{id}/enable")
    public FieldDefinition enable(@PathVariable Long id) {
        return fieldDefinitionService.enable(id);
    }

    @PostMapping("/{id}/disable")
    public FieldDefinition disable(@PathVariable Long id) {
        return fieldDefinitionService.disable(id);
    }

    @GetMapping("/database/tables")
    public List<DatabaseTableResponse> listDatabaseTables() {
        return fieldDefinitionService.listDatabaseTables();
    }

    @GetMapping("/database/tables/{tableName}/columns")
    public List<DatabaseColumnResponse> listDatabaseColumns(@PathVariable String tableName) {
        return fieldDefinitionService.listDatabaseColumns(tableName);
    }

    @PostMapping("/import-db")
    public List<FieldDefinition> importDatabaseFields(@RequestBody DatabaseFieldImportRequest request) {
        return fieldDefinitionService.importDatabaseFields(request);
    }
}
