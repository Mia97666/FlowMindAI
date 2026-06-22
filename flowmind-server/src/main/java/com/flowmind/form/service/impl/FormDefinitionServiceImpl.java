package com.flowmind.form.service.impl;

import com.flowmind.common.exception.ConflictException;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.form.dto.FormDefinitionRequest;
import com.flowmind.form.entity.FormDefinition;
import com.flowmind.form.entity.FormVersion;
import com.flowmind.form.repository.FormDefinitionRepository;
import com.flowmind.form.repository.FormVersionRepository;
import com.flowmind.form.service.FormDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表单定义服务实现。
 */
@Service
@RequiredArgsConstructor
public class FormDefinitionServiceImpl implements FormDefinitionService {

    private static final String STATUS_DRAFT = "DRAFT";

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private static final String STATUS_DISABLED = "DISABLED";

    private final FormDefinitionRepository repository;

    private final FormVersionRepository versionRepository;

    @Override
    public List<FormDefinition> list(
            String formCode,
            String formName,
            String category,
            String status,
            Boolean enabled
    ) {
        return repository.findAll(formSpec(formCode, formName, category, status, enabled));
    }

    @Override
    public Page<FormDefinition> queryPage(
            String formCode,
            String formName,
            String category,
            String status,
            Pageable pageable
    ) {
        return repository.findAll(formSpec(formCode, formName, category, null, null), pageable);
    }

    private Specification<FormDefinition> formSpec(
            String formCode,
            String formName,
            String category,
            String status,
            Boolean enabled
    ) {
        SpecificationBuilder<FormDefinition> builder = SpecificationBuilder.<FormDefinition>builder()
                .like("formCode", formCode)
                .like("formName", formName)
                .equal("category", category)
                .equal("status", status);
        Specification<FormDefinition> spec = builder.build();
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        return spec;
    }

    @Override
    public FormDefinition get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("表单", id));
    }

    @Override
    @Transactional
    public FormDefinition create(FormDefinitionRequest request) {
        validateRequired(request.getFormCode(), "表单编码不能为空");
        validateRequired(request.getFormName(), "表单名称不能为空");
        repository.findByFormCode(request.getFormCode()).ifPresent(form -> {
            throw new ConflictException("表单编码已存在：" + form.getFormCode());
        });

        FormDefinition definition = new FormDefinition();
        fillDefinition(definition, request);
        definition.setVersion(request.getVersion() == null ? 1 : request.getVersion());
        definition.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_DRAFT);
        definition.setEnabled(STATUS_PUBLISHED.equals(definition.getStatus()));
        definition.setPublishedAt(STATUS_PUBLISHED.equals(definition.getStatus()) ? LocalDateTime.now() : null);
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        FormDefinition savedDefinition = repository.save(definition);
        if (STATUS_PUBLISHED.equals(savedDefinition.getStatus())) {
            publish(savedDefinition.getId());
            return get(savedDefinition.getId());
        }
        return savedDefinition;
    }

    @Override
    @Transactional
    public FormDefinition update(Long id, FormDefinitionRequest request) {
        FormDefinition definition = get(id);
        validateRequired(request.getFormCode(), "表单编码不能为空");
        validateRequired(request.getFormName(), "表单名称不能为空");
        repository.findByFormCode(request.getFormCode())
                .filter(form -> !form.getId().equals(id))
                .ifPresent(form -> {
                    throw new ConflictException("表单编码已存在：" + form.getFormCode());
                });

        fillDefinition(definition, request);
        if (request.getVersion() != null) {
            definition.setVersion(request.getVersion());
        }
        if (StringUtils.hasText(request.getStatus())) {
            definition.setStatus(request.getStatus());
            definition.setEnabled(STATUS_PUBLISHED.equals(request.getStatus()));
            if (STATUS_PUBLISHED.equals(request.getStatus()) && definition.getPublishedAt() == null) {
                definition.setPublishedAt(LocalDateTime.now());
            }
        }
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    @Transactional
    public FormDefinition publish(Long id) {
        FormDefinition definition = get(id);
        FormVersion version = createVersion(definition);
        definition.setStatus(STATUS_PUBLISHED);
        definition.setEnabled(true);
        definition.setVersion(version.getVersion());
        definition.setPublishedVersionId(version.getId());
        definition.setPublishedAt(version.getPublishedAt());
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    public List<FormVersion> listVersions(Long id) {
        get(id);
        return versionRepository.findByFormDefinitionIdOrderByVersionDesc(id);
    }

    @Override
    @Transactional
    public FormDefinition enable(Long id) {
        FormDefinition definition = get(id);
        definition.setEnabled(true);
        if (STATUS_DISABLED.equals(definition.getStatus())) {
            definition.setStatus(STATUS_PUBLISHED);
        }
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    @Transactional
    public FormDefinition disable(Long id) {
        FormDefinition definition = get(id);
        definition.setStatus(STATUS_DISABLED);
        definition.setEnabled(false);
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    private void fillDefinition(FormDefinition definition, FormDefinitionRequest request) {
        definition.setFormCode(request.getFormCode());
        definition.setFormName(request.getFormName());
        definition.setCategory(request.getCategory());
        definition.setSchemaJson(request.getSchemaJson());
        definition.setDescription(request.getDescription());
    }

    private FormVersion createVersion(FormDefinition definition) {
        LocalDateTime now = LocalDateTime.now();
        Integer nextVersion = versionRepository.findTopByFormDefinitionIdOrderByVersionDesc(definition.getId())
                .map(FormVersion::getVersion)
                .map(version -> version + 1)
                .orElse(1);

        FormVersion version = new FormVersion();
        version.setFormDefinitionId(definition.getId());
        version.setFormCode(definition.getFormCode());
        version.setFormName(definition.getFormName());
        version.setCategory(definition.getCategory());
        version.setVersion(nextVersion);
        version.setStatus(STATUS_PUBLISHED);
        version.setSchemaJson(definition.getSchemaJson());
        version.setDescription(definition.getDescription());
        version.setPublishedAt(now);
        version.setCreatedAt(now);
        return versionRepository.save(version);
    }

    private void validateRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
