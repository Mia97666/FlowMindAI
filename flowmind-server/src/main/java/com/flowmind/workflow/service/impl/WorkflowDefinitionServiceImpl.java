package com.flowmind.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.form.entity.FormVersion;
import com.flowmind.form.repository.FormVersionRepository;
import com.flowmind.workflow.dto.WorkflowDefinitionRequest;
import com.flowmind.workflow.dto.WorkflowPreCheckResult;
import com.flowmind.workflow.engine.model.WorkflowEdge;
import com.flowmind.workflow.engine.model.WorkflowGraph;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.engine.support.WorkflowGraphParser;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowVersion;
import com.flowmind.workflow.repository.WorkflowDefinitionRepository;
import com.flowmind.workflow.repository.WorkflowVersionRepository;
import com.flowmind.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 工作流定义服务
 */
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionServiceImpl
        implements WorkflowDefinitionService {

    private static final String STATUS_DRAFT = "DRAFT";

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private static final String STATUS_DISABLED = "DISABLED";

    private final WorkflowDefinitionRepository repository;

    private final WorkflowVersionRepository versionRepository;

    private final FormVersionRepository formVersionRepository;

    private final WorkflowGraphParser graphParser;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public WorkflowDefinition create(
            WorkflowDefinitionRequest request
    ) {

        WorkflowDefinition definition =
                new WorkflowDefinition();

        definition.setCode(request.getCode());
        definition.setName(request.getName());
        definition.setDescription(
                request.getDescription()
        );

        definition.setDefinitionJson(
                request.getDefinitionJson()
        );

        definition.setFormJson(request.getFormJson());
        definition.setBpmnXml(request.getBpmnXml());
        definition.setVersion(request.getVersion() == null ? 1 : request.getVersion());
        definition.setStatus(request.getStatus() == null ? STATUS_PUBLISHED : request.getStatus());
        definition.setEnabled(!STATUS_DISABLED.equals(definition.getStatus()));
        definition.setPublishedAt(STATUS_PUBLISHED.equals(definition.getStatus())
                ? LocalDateTime.now()
                : null);

        definition.setCreatedAt(
                LocalDateTime.now()
        );

        definition.setUpdatedAt(
                LocalDateTime.now()
        );

        WorkflowDefinition savedDefinition = repository.save(definition);
        if (STATUS_PUBLISHED.equals(savedDefinition.getStatus())) {
            publish(savedDefinition.getId());
            return get(savedDefinition.getId());
        }
        return savedDefinition;
    }

    @Override
    @Transactional
    public WorkflowDefinition update(
            Long id,
            WorkflowDefinitionRequest request
    ) {
        WorkflowDefinition definition = get(id);

        definition.setCode(request.getCode() != null ? request.getCode() : definition.getCode());
        definition.setName(request.getName() != null ? request.getName() : definition.getName());
        definition.setDescription(request.getDescription() != null ? request.getDescription() : definition.getDescription());
        definition.setFormJson(request.getFormJson() != null ? request.getFormJson() : definition.getFormJson());
        definition.setDefinitionJson(request.getDefinitionJson() != null ? request.getDefinitionJson() : definition.getDefinitionJson());
        definition.setBpmnXml(request.getBpmnXml() != null ? request.getBpmnXml() : definition.getBpmnXml());

        if (request.getVersion() != null) {
            definition.setVersion(request.getVersion());
        }
        if (request.getStatus() != null) {
            definition.setStatus(request.getStatus());
            definition.setEnabled(!STATUS_DISABLED.equals(request.getStatus()));
        }

        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    public List<WorkflowDefinition> list() {
        return repository.findAll();
    }

    @Override
    public List<WorkflowDefinition> listEnabled() {
        return repository.findByEnabledTrueOrderByUpdatedAtDesc();
    }

    @Override
    public Page<WorkflowDefinition> queryPage(
            String name,
            String code,
            String status,
            String category,
            Pageable pageable
    ) {
        Specification<WorkflowDefinition> specification = SpecificationBuilder.<WorkflowDefinition>builder()
                .like("name", name)
                .like("code", code)
                .equal("status", status)
                .like("category", category)
                .build();
        return repository.findAll(specification, pageable);
    }

    @Override
    public WorkflowDefinition get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("流程", id));
    }

    @Override
    public WorkflowPreCheckResult preCheck(
            Long id,
            WorkflowDefinitionRequest request
    ) {
        WorkflowDefinition definition = get(id);
        String definitionJson = request != null
                && request.getDefinitionJson() != null
                && !request.getDefinitionJson().isBlank()
                ? request.getDefinitionJson()
                : definition.getDefinitionJson();

        WorkflowGraph graph = graphParser.parse(definitionJson);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateNodes(graph, errors, warnings);
        validateEdges(graph, errors);
        validateConnectivity(graph, errors);

        return WorkflowPreCheckResult.of(errors, warnings);
    }

    @Override
    @Transactional
    public WorkflowDefinition publish(Long id) {
        WorkflowDefinition definition = get(id);
        WorkflowVersion version = createVersion(definition);
        definition.setStatus(STATUS_PUBLISHED);
        definition.setEnabled(true);
        definition.setVersion(version.getVersion());
        definition.setPublishedVersionId(version.getId());
        definition.setPublishedAt(version.getPublishedAt());
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    public List<WorkflowVersion> listVersions(Long id) {
        get(id);
        return versionRepository.findByDefinitionIdOrderByVersionDesc(id);
    }

    @Override
    public WorkflowDefinition enable(Long id) {
        WorkflowDefinition definition = get(id);
        definition.setEnabled(true);
        if (STATUS_DISABLED.equals(definition.getStatus())) {
            definition.setStatus(STATUS_PUBLISHED);
        }
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    @Override
    public WorkflowDefinition disable(Long id) {
        WorkflowDefinition definition = get(id);
        definition.setEnabled(false);
        definition.setStatus(STATUS_DISABLED);
        definition.setUpdatedAt(LocalDateTime.now());
        return repository.save(definition);
    }

    private void validateNodes(
            WorkflowGraph graph,
            List<String> errors,
            List<String> warnings
    ) {
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            errors.add("流程图至少需要一个开始节点和一个结束节点");
            return;
        }

        Set<String> nodeIds = new HashSet<>();
        int startCount = 0;
        int endCount = 0;

        for (WorkflowNode node : graph.getNodes()) {
            if (node.getId() == null || node.getId().isBlank()) {
                errors.add("存在未配置节点ID的节点");
                continue;
            }
            if (!nodeIds.add(node.getId())) {
                errors.add("节点ID重复：" + node.getId());
            }
            String type = normalizeType(node.getType());
            if ("START".equals(type)) {
                startCount++;
            }
            if ("END".equals(type)) {
                endCount++;
            }
            validateNodeConfig(node, type, errors, warnings);
        }

        if (startCount == 0) {
            errors.add("缺少开始节点");
        } else if (startCount > 1) {
            errors.add("开始节点只能有一个");
        }
        if (endCount == 0) {
            errors.add("缺少结束节点");
        }
    }

    private void validateNodeConfig(
            WorkflowNode node,
            String type,
            List<String> errors,
            List<String> warnings
    ) {
        Map<String, Object> config = node.getConfig() == null
                ? Collections.emptyMap()
                : node.getConfig();
        String nodeName = node.getName() == null ? node.getId() : node.getName();

        if (needsForm(type) && isBlank(config.get("formCode"))) {
            errors.add("节点【" + nodeName + "】缺少绑定表单");
        }
        if (("APPROVAL".equals(type) || "AI_APPROVAL".equals(type))
                && isBlank(config.get("assigneeType"))) {
            errors.add("节点【" + nodeName + "】缺少审批人类型");
        }
        if (("APPROVAL".equals(type) || "AI_APPROVAL".equals(type))
                && needsAssigneeValue(config)
                && isBlank(config.get("assigneeValue"))) {
            errors.add("节点【" + nodeName + "】缺少审批人、角色或表单字段");
        }
        if (("AI_RISK_CHECK".equals(type) || "AI_APPROVAL".equals(type))
                && isBlank(config.get("aiStrategy"))) {
            errors.add("节点【" + nodeName + "】缺少 AI 策略");
        }
        if ("CONDITION".equals(type) && isBlank(config.get("defaultRoute"))) {
            warnings.add("节点【" + nodeName + "】未配置默认路由，执行时将依赖连线顺序");
        }
        if ("NOTIFY".equals(type) && isBlank(config.get("receivers"))) {
            warnings.add("节点【" + nodeName + "】未配置通知接收人");
        }
    }

    private void validateEdges(
            WorkflowGraph graph,
            List<String> errors
    ) {
        Set<String> nodeIds = new HashSet<>();
        for (WorkflowNode node : graph.getNodes()) {
            nodeIds.add(node.getId());
        }

        Set<String> edgeKeys = new HashSet<>();
        for (WorkflowEdge edge : graph.getEdges()) {
            if (!nodeIds.contains(edge.getSource())) {
                errors.add("连线【" + edge.getId() + "】来源节点不存在：" + edge.getSource());
            }
            if (!nodeIds.contains(edge.getTarget())) {
                errors.add("连线【" + edge.getId() + "】目标节点不存在：" + edge.getTarget());
            }
            String edgeKey = edge.getSource() + "->" + edge.getTarget();
            if (!edgeKeys.add(edgeKey)) {
                errors.add("重复连线：" + edgeKey);
            }
        }
    }

    private void validateConnectivity(
            WorkflowGraph graph,
            List<String> errors
    ) {
        for (WorkflowNode node : graph.getNodes()) {
            String type = normalizeType(node.getType());
            String nodeName = node.getName() == null ? node.getId() : node.getName();
            boolean hasIncoming = graph.getEdges().stream()
                    .anyMatch(edge -> node.getId().equals(edge.getTarget()));
            boolean hasOutgoing = graph.getEdges().stream()
                    .anyMatch(edge -> node.getId().equals(edge.getSource()));

            if (!"START".equals(type) && !hasIncoming) {
                errors.add("节点【" + nodeName + "】没有入边");
            }
            if (!"END".equals(type) && !hasOutgoing) {
                errors.add("节点【" + nodeName + "】没有出边");
            }
        }
    }

    private boolean needsForm(String type) {
        return "START".equals(type)
                || "FORM_TASK".equals(type)
                || "APPROVAL".equals(type)
                || "AI_RISK_CHECK".equals(type)
                || "AI_APPROVAL".equals(type);
    }

    private boolean needsAssigneeValue(Map<String, Object> config) {
        Object assigneeType = config.get("assigneeType");
        return assigneeType != null
                && !"MANAGER".equals(String.valueOf(assigneeType))
                && !"DEPARTMENT_MANAGER".equals(String.valueOf(assigneeType));
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }

    private WorkflowVersion createVersion(WorkflowDefinition definition) {
        LocalDateTime now = LocalDateTime.now();
        Integer nextVersion = versionRepository.findTopByDefinitionIdOrderByVersionDesc(definition.getId())
                .map(WorkflowVersion::getVersion)
                .map(version -> version + 1)
                .orElse(1);

        WorkflowVersion version = new WorkflowVersion();
        version.setDefinitionId(definition.getId());
        version.setCode(definition.getCode());
        version.setName(definition.getName());
        version.setDescription(definition.getDescription());
        version.setVersion(nextVersion);
        version.setStatus(STATUS_PUBLISHED);
        version.setFormJson(definition.getFormJson());
        version.setDefinitionJson(enrichDefinitionJsonWithFormVersions(definition.getDefinitionJson()));
        version.setBpmnXml(definition.getBpmnXml());
        version.setPublishedAt(now);
        version.setCreatedAt(now);
        return versionRepository.save(version);
    }

    /**
     * 发布工作流时，把节点绑定的 formCode 固化成 formVersionId。
     *
     * 这样实例启动后即使管理员重新发布表单，历史实例也仍然使用当时的表单版本。
     */
    private String enrichDefinitionJsonWithFormVersions(String definitionJson) {
        if (definitionJson == null || definitionJson.isBlank()) {
            return definitionJson;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    definitionJson,
                    new TypeReference<>() {}
            );
            Object rawNodes = root.get("nodes");
            if (rawNodes instanceof List<?> nodes) {
                for (Object item : nodes) {
                    if (item instanceof Map<?, ?> rawNode) {
                        enrichNodeConfigWithFormVersion((Map<String, Object>) rawNode);
                    }
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalArgumentException("工作流发布快照生成失败：" + exception.getMessage(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichNodeConfigWithFormVersion(Map<String, Object> rawNode) {
        Map<String, Object> config = resolveMutableConfig(rawNode);
        Object formCodeValue = config.get("formCode");
        if (formCodeValue == null || String.valueOf(formCodeValue).isBlank()) {
            return;
        }
        formVersionRepository.findTopByFormCodeOrderByVersionDesc(String.valueOf(formCodeValue))
                .ifPresent(version -> applyFormVersion(config, version));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveMutableConfig(Map<String, Object> rawNode) {
        Object directConfig = rawNode.get("config");
        if (directConfig instanceof Map<?, ?> directMap) {
            return (Map<String, Object>) directMap;
        }
        Object data = rawNode.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object dataConfig = ((Map<String, Object>) dataMap).get("config");
            if (dataConfig instanceof Map<?, ?> configMap) {
                return (Map<String, Object>) configMap;
            }
        }
        Map<String, Object> config = new java.util.LinkedHashMap<>();
        rawNode.put("config", config);
        return config;
    }

    private void applyFormVersion(
            Map<String, Object> config,
            FormVersion version
    ) {
        config.put("formVersionId", version.getId());
        config.put("formVersion", version.getVersion());
        config.put("formName", version.getFormName());
    }
}
