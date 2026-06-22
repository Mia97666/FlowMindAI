package com.flowmind.form.service;

import com.flowmind.form.dto.FormDefinitionRequest;
import com.flowmind.form.entity.FormDefinition;
import com.flowmind.form.entity.FormVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 表单定义服务。
 */
public interface FormDefinitionService {

    List<FormDefinition> list(
            String formCode,
            String formName,
            String category,
            String status,
            Boolean enabled
    );

    Page<FormDefinition> queryPage(
            String formCode,
            String formName,
            String category,
            String status,
            Pageable pageable
    );

    FormDefinition get(Long id);

    FormDefinition create(FormDefinitionRequest request);

    FormDefinition update(Long id, FormDefinitionRequest request);

    FormDefinition publish(Long id);

    List<FormVersion> listVersions(Long id);

    FormDefinition enable(Long id);

    FormDefinition disable(Long id);
}
