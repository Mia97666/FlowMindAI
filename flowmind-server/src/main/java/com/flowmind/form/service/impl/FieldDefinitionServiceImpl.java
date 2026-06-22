package com.flowmind.form.service.impl;

import com.flowmind.common.exception.ConflictException;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.form.dto.DatabaseColumnResponse;
import com.flowmind.form.dto.DatabaseFieldImportItem;
import com.flowmind.form.dto.DatabaseFieldImportRequest;
import com.flowmind.form.dto.DatabaseTableResponse;
import com.flowmind.form.dto.FieldDefinitionRequest;
import com.flowmind.form.entity.FieldDefinition;
import com.flowmind.form.repository.FieldDefinitionRepository;
import com.flowmind.form.service.FieldDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 字段定义服务实现。
 */
@Service
@RequiredArgsConstructor
public class FieldDefinitionServiceImpl implements FieldDefinitionService {

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String STATUS_DISABLED = "DISABLED";

    private static final String SOURCE_CUSTOM = "CUSTOM";

    private static final String SOURCE_DB_COLUMN = "DB_COLUMN";

    private static final String DEFAULT_SCHEMA = "public";

    private final FieldDefinitionRepository repository;

    private final DataSource dataSource;

    @Override
    public List<FieldDefinition> list(
            String fieldKey,
            String fieldName,
            String fieldType,
            String sourceType,
            String status
    ) {
        return repository.findAll(fieldSpec(fieldKey, fieldName, fieldType, sourceType, status));
    }

    @Override
    public Page<FieldDefinition> queryPage(
            String fieldKey,
            String fieldName,
            String fieldType,
            String sourceType,
            String status,
            Pageable pageable
    ) {
        return repository.findAll(fieldSpec(fieldKey, fieldName, fieldType, sourceType, status), pageable);
    }

    private Specification<FieldDefinition> fieldSpec(
            String fieldKey,
            String fieldName,
            String fieldType,
            String sourceType,
            String status
    ) {
        return SpecificationBuilder.<FieldDefinition>builder()
                .like("fieldKey", fieldKey)
                .like("fieldName", fieldName)
                .equal("fieldType", fieldType)
                .equal("sourceType", sourceType)
                .equal("status", status)
                .build();
    }

    @Override
    public FieldDefinition get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("字段", id));
    }

    @Override
    @Transactional
    public FieldDefinition create(FieldDefinitionRequest request) {
        validateRequired(request.getFieldKey(), "字段 Key 不能为空");
        validateRequired(request.getFieldName(), "字段名称不能为空");
        validateRequired(request.getFieldType(), "字段类型不能为空");
        repository.findByFieldKey(request.getFieldKey()).ifPresent(field -> {
            throw new ConflictException("字段 Key 已存在：" + field.getFieldKey());
        });

        FieldDefinition definition = new FieldDefinition();
        fillDefinition(definition, request);
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    @Transactional
    public FieldDefinition update(Long id, FieldDefinitionRequest request) {
        FieldDefinition definition = get(id);
        validateRequired(request.getFieldKey(), "字段 Key 不能为空");
        validateRequired(request.getFieldName(), "字段名称不能为空");
        validateRequired(request.getFieldType(), "字段类型不能为空");
        repository.findByFieldKey(request.getFieldKey())
                .filter(field -> !field.getId().equals(id))
                .ifPresent(field -> {
                    throw new ConflictException("字段 Key 已存在：" + field.getFieldKey());
                });

        fillDefinition(definition, request);
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    @Transactional
    public FieldDefinition enable(Long id) {
        FieldDefinition definition = get(id);
        definition.setStatus(STATUS_ENABLED);
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    @Transactional
    public FieldDefinition disable(Long id) {
        FieldDefinition definition = get(id);
        definition.setStatus(STATUS_DISABLED);
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    public List<DatabaseTableResponse> listDatabaseTables() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<DatabaseTableResponse> tables = readTables(metaData, DEFAULT_SCHEMA);
            if (tables.isEmpty()) {
                tables = readTables(metaData, null);
            }
            tables.sort(Comparator.comparing(DatabaseTableResponse::getTableName));
            return tables;
        } catch (SQLException exception) {
            throw new IllegalStateException("读取数据库表元数据失败", exception);
        }
    }

    @Override
    public List<DatabaseColumnResponse> listDatabaseColumns(String tableName) {
        validateRequired(tableName, "表名不能为空");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<DatabaseColumnResponse> columns = readColumns(metaData, DEFAULT_SCHEMA, tableName);
            if (columns.isEmpty()) {
                columns = readColumns(metaData, null, tableName);
            }
            return columns;
        } catch (SQLException exception) {
            throw new IllegalStateException("读取数据库列元数据失败", exception);
        }
    }

    @Override
    @Transactional
    public List<FieldDefinition> importDatabaseFields(DatabaseFieldImportRequest request) {
        validateRequired(request.getSourceTable(), "来源表不能为空");
        if (request.getColumns() == null || request.getColumns().isEmpty()) {
            throw new IllegalArgumentException("至少选择一个数据库字段");
        }

        List<FieldDefinition> result = new ArrayList<>();
        for (DatabaseFieldImportItem item : request.getColumns()) {
            validateRequired(item.getSourceColumn(), "来源列不能为空");
            validateRequired(item.getFieldKey(), "字段 Key 不能为空");
            validateRequired(item.getFieldName(), "字段名称不能为空");

            // 数据库字段导入设计为幂等操作：同一个 fieldKey 再次导入时更新映射信息，避免重复记录。
            FieldDefinition definition = repository.findByFieldKey(item.getFieldKey())
                    .orElseGet(() -> {
                        FieldDefinition newDefinition = new FieldDefinition();
                        newDefinition.setCreatedAt(LocalDateTime.now());
                        return newDefinition;
                    });
            definition.setFieldKey(item.getFieldKey());
            definition.setFieldName(item.getFieldName());
            definition.setFieldType(StringUtils.hasText(item.getFieldType()) ? item.getFieldType() : "TEXT");
            definition.setSourceType(SOURCE_DB_COLUMN);
            definition.setSourceTable(request.getSourceTable());
            definition.setSourceColumn(item.getSourceColumn());
            definition.setStatus(STATUS_ENABLED);
            definition.setUpdatedAt(LocalDateTime.now());
            result.add(repository.save(definition));
        }
        return result;
    }

    private List<DatabaseTableResponse> readTables(DatabaseMetaData metaData, String schema) throws SQLException {
        List<DatabaseTableResponse> tables = new ArrayList<>();
        try (ResultSet resultSet = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
            while (resultSet.next()) {
                tables.add(new DatabaseTableResponse(
                        resultSet.getString("TABLE_NAME"),
                        resultSet.getString("TABLE_TYPE"),
                        resultSet.getString("REMARKS")
                ));
            }
        }
        return tables;
    }

    private List<DatabaseColumnResponse> readColumns(
            DatabaseMetaData metaData,
            String schema,
            String tableName
    ) throws SQLException {
        List<DatabaseColumnResponse> columns = new ArrayList<>();
        try (ResultSet resultSet = metaData.getColumns(null, schema, tableName, "%")) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                Integer dataType = resultSet.getInt("DATA_TYPE");
                String remarks = resultSet.getString("REMARKS");
                columns.add(new DatabaseColumnResponse(
                        columnName,
                        dataType,
                        resultSet.getString("TYPE_NAME"),
                        buildSuggestedFieldKey(tableName, columnName),
                        StringUtils.hasText(remarks) ? remarks : columnName,
                        mapJdbcType(dataType),
                        remarks
                ));
            }
        }
        return columns;
    }

    private void fillDefinition(FieldDefinition definition, FieldDefinitionRequest request) {
        definition.setFieldKey(request.getFieldKey());
        definition.setFieldName(request.getFieldName());
        definition.setFieldType(request.getFieldType());
        definition.setSourceType(StringUtils.hasText(request.getSourceType()) ? request.getSourceType() : SOURCE_CUSTOM);
        definition.setSourceTable(request.getSourceTable());
        definition.setSourceColumn(request.getSourceColumn());
        definition.setDefaultValue(request.getDefaultValue());
        definition.setOptionsJson(request.getOptionsJson());
        definition.setValidationJson(request.getValidationJson());
        definition.setDescription(request.getDescription());
        definition.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_ENABLED);
    }

    private String buildSuggestedFieldKey(String tableName, String columnName) {
        return (tableName + "_" + columnName)
                .replaceAll("[^A-Za-z0-9_]", "_")
                .toLowerCase(Locale.ROOT);
    }

    private String mapJdbcType(Integer dataType) {
        if (dataType == null) {
            return "TEXT";
        }
        return switch (dataType) {
            case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT,
                    Types.FLOAT, Types.REAL, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> "NUMBER";
            case Types.DATE -> "DATE";
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, Types.TIME, Types.TIME_WITH_TIMEZONE -> "DATETIME";
            case Types.BOOLEAN, Types.BIT -> "BOOLEAN";
            case Types.CLOB, Types.NCLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR -> "TEXTAREA";
            default -> "TEXT";
        };
    }

    private void validateRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
