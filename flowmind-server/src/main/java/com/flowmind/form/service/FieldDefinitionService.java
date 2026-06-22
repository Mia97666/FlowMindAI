package com.flowmind.form.service;

import com.flowmind.form.dto.DatabaseColumnResponse;
import com.flowmind.form.dto.DatabaseFieldImportRequest;
import com.flowmind.form.dto.DatabaseTableResponse;
import com.flowmind.form.dto.FieldDefinitionRequest;
import com.flowmind.form.entity.FieldDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 表单字段定义服务。
 */
public interface FieldDefinitionService {

    List<FieldDefinition> list(
            String fieldKey,
            String fieldName,
            String fieldType,
            String sourceType,
            String status
    );

    Page<FieldDefinition> queryPage(
            String fieldKey,
            String fieldName,
            String fieldType,
            String sourceType,
            String status,
            Pageable pageable
    );

    FieldDefinition get(Long id);

    FieldDefinition create(FieldDefinitionRequest request);

    FieldDefinition update(Long id, FieldDefinitionRequest request);

    FieldDefinition enable(Long id);

    FieldDefinition disable(Long id);

    List<DatabaseTableResponse> listDatabaseTables();

    List<DatabaseColumnResponse> listDatabaseColumns(String tableName);

    List<FieldDefinition> importDatabaseFields(DatabaseFieldImportRequest request);
}
