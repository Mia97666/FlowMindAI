package com.flowmind.form.repository;

import com.flowmind.form.entity.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 字段定义数据访问。
 */
public interface FieldDefinitionRepository
        extends JpaRepository<FieldDefinition, Long>, JpaSpecificationExecutor<FieldDefinition> {

    Optional<FieldDefinition> findByFieldKey(String fieldKey);
}
