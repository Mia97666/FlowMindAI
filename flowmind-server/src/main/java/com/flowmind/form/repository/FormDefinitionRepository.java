package com.flowmind.form.repository;

import com.flowmind.form.entity.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 表单定义数据访问。
 */
public interface FormDefinitionRepository
        extends JpaRepository<FormDefinition, Long>, JpaSpecificationExecutor<FormDefinition> {

    Optional<FormDefinition> findByFormCode(String formCode);

    List<FormDefinition> findByEnabledTrueOrderByUpdatedAtDesc();
}
