package com.flowmind.form.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.form.dto.RuntimeFormResponse;
import com.flowmind.form.entity.FormDefinition;
import com.flowmind.form.entity.FormVersion;
import com.flowmind.form.repository.FormDefinitionRepository;
import com.flowmind.form.repository.FormVersionRepository;
import com.flowmind.form.service.RuntimeFormService;
import com.flowmind.workflow.engine.model.WorkflowGraph;
import com.flowmind.workflow.engine.model.WorkflowNode;
import com.flowmind.workflow.engine.support.WorkflowGraphParser;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowInstance;
import com.flowmind.workflow.entity.WorkflowTask;
import com.flowmind.workflow.entity.WorkflowVersion;
import com.flowmind.workflow.repository.WorkflowDefinitionRepository;
import com.flowmind.workflow.repository.WorkflowInstanceRepository;
import com.flowmind.workflow.repository.WorkflowTaskRepository;
import com.flowmind.workflow.repository.WorkflowVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时表单解析服务实现。
 */
@Service
@RequiredArgsConstructor
public class RuntimeFormServiceImpl implements RuntimeFormService {

    private static final String EMPTY_SCHEMA = "{\"fields\":[]}";

    private final WorkflowDefinitionRepository definitionRepository;

    private final WorkflowVersionRepository workflowVersionRepository;

    private final WorkflowInstanceRepository instanceRepository;

    private final WorkflowTaskRepository taskRepository;

    private final FormDefinitionRepository formDefinitionRepository;

    private final FormVersionRepository formVersionRepository;

    private final WorkflowGraphParser graphParser;

    private final ObjectMapper objectMapper;

    @Override
    public RuntimeFormResponse resolveStartForm(Long definitionId) {
        WorkflowDefinition definition = getExecutableDefinition(getDefinition(definitionId));
        WorkflowGraph graph = graphParser.parse(definition.getDefinitionJson());
        WorkflowNode startNode = graphParser.findStartNode(graph);
        return buildResponse(definition, null, null, startNode, Map.of(), false);
    }

    @Override
    public RuntimeFormResponse resolveTaskForm(Long taskId) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
        WorkflowInstance instance = getInstance(task.getInstanceId());
        WorkflowDefinition definition = getExecutableDefinition(instance);
        WorkflowNode node = findNode(definition, task.getNodeId());
        RuntimeFormResponse response = buildResponse(
                definition,
                instance,
                task,
                node,
                parseBusinessData(instance.getBusinessDataJson()),
                !"PENDING".equals(task.getStatus())
        );
        response.setTaskId(task.getId());
        return response;
    }

    @Override
    public RuntimeFormResponse resolveInstanceForm(Long instanceId, String nodeId) {
        WorkflowInstance instance = getInstance(instanceId);
        WorkflowDefinition definition = getExecutableDefinition(instance);
        String targetNodeId = StringUtils.hasText(nodeId) ? nodeId : instance.getCurrentNodeId();
        WorkflowNode node = findNode(definition, targetNodeId);
        return buildResponse(
                definition,
                instance,
                null,
                node,
                parseBusinessData(instance.getBusinessDataJson()),
                true
        );
    }

    private RuntimeFormResponse buildResponse(
            WorkflowDefinition definition,
            WorkflowInstance instance,
            WorkflowTask task,
            WorkflowNode node,
            Map<String, Object> businessData,
            boolean readOnly
    ) {
        RuntimeFormResponse response = new RuntimeFormResponse();
        response.setDefinitionId(definition.getId());
        response.setDefinitionVersionId(definition.getPublishedVersionId());
        response.setDefinitionVersion(definition.getVersion());
        response.setInstanceId(instance == null ? null : instance.getId());
        response.setTaskId(task == null ? null : task.getId());
        response.setNodeId(node == null ? null : node.getId());
        response.setNodeName(node == null ? null : node.getName());
        response.setReadOnly(readOnly);
        response.setBusinessData(new LinkedHashMap<>(businessData));

        String formCode = resolveFormCode(node);
        Long formVersionId = resolveFormVersionId(node);
        if (task != null && task.getBindFormVersionId() != null) {
            formVersionId = task.getBindFormVersionId();
        } else if (instance != null
                && instance.getStartFormVersionId() != null
                && (node == null || "START".equalsIgnoreCase(node.getType()))) {
            formVersionId = instance.getStartFormVersionId();
        }

        if (formVersionId != null) {
            formVersionRepository.findById(formVersionId)
                    .ifPresent(formVersion -> applyFormVersion(response, formVersion));
        }

        if (!StringUtils.hasText(formCode)) {
            formCode = findFirstFormCode(definition);
        }

        if (!StringUtils.hasText(response.getSchemaJson()) && StringUtils.hasText(formCode)) {
            formVersionRepository.findTopByFormCodeOrderByVersionDesc(formCode)
                    .ifPresent(formVersion -> applyFormVersion(response, formVersion));
        }

        if (!StringUtils.hasText(response.getSchemaJson()) && StringUtils.hasText(formCode)) {
            formDefinitionRepository.findByFormCode(formCode).ifPresent(form -> applyForm(response, form));
        }

        // 如果节点没有绑定表单，或绑定的表单不存在，降级使用流程定义里的旧 formJson。
        if (!StringUtils.hasText(response.getSchemaJson())) {
            response.setFormCode(StringUtils.hasText(formCode) ? formCode : definition.getCode() + "_INLINE_FORM");
            response.setFormName(definition.getName() + "内置表单");
            response.setCategory("兼容表单");
            response.setSchemaJson(StringUtils.hasText(definition.getFormJson()) ? definition.getFormJson() : EMPTY_SCHEMA);
            response.setFallback(true);
        } else {
            response.setFallback(false);
        }
        applyFieldPermissions(response, node, readOnly);
        return response;
    }

    private void applyForm(RuntimeFormResponse response, FormDefinition form) {
        response.setFormCode(form.getFormCode());
        response.setFormName(form.getFormName());
        response.setCategory(form.getCategory());
        response.setSchemaJson(StringUtils.hasText(form.getSchemaJson()) ? form.getSchemaJson() : EMPTY_SCHEMA);
    }

    private void applyFormVersion(RuntimeFormResponse response, FormVersion formVersion) {
        response.setFormCode(formVersion.getFormCode());
        response.setFormVersionId(formVersion.getId());
        response.setFormVersion(formVersion.getVersion());
        response.setFormName(formVersion.getFormName());
        response.setCategory(formVersion.getCategory());
        response.setSchemaJson(StringUtils.hasText(formVersion.getSchemaJson())
                ? formVersion.getSchemaJson()
                : EMPTY_SCHEMA);
    }

    private WorkflowNode findNode(WorkflowDefinition definition, String nodeId) {
        WorkflowGraph graph = graphParser.parse(definition.getDefinitionJson());
        if (!StringUtils.hasText(nodeId)) {
            return graphParser.findStartNode(graph);
        }
        WorkflowNode node = graphParser.findNode(graph, nodeId);
        return node == null ? graphParser.findStartNode(graph) : node;
    }

    private String resolveFormCode(WorkflowNode node) {
        if (node == null || node.getConfig() == null) {
            return null;
        }
        Object formCode = node.getConfig().get("formCode");
        return formCode == null ? null : String.valueOf(formCode);
    }

    private Long resolveFormVersionId(WorkflowNode node) {
        if (node == null || node.getConfig() == null) {
            return null;
        }
        Object formVersionId = node.getConfig().get("formVersionId");
        if (formVersionId == null || String.valueOf(formVersionId).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(formVersionId));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String findFirstFormCode(WorkflowDefinition definition) {
        WorkflowGraph graph = graphParser.parse(definition.getDefinitionJson());
        return graph.getNodes().stream()
                .map(this::resolveFormCode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    /**
     * 将节点字段权限合并到运行时表单 Schema。
     *
     * 节点配置格式：
     * fieldPermissions: {"amount":"READONLY","purpose":"EDITABLE","invoiceNo":"HIDDEN"}
     */
    private void applyFieldPermissions(
            RuntimeFormResponse response,
            WorkflowNode node,
            boolean readOnly
    ) {
        if (!StringUtils.hasText(response.getSchemaJson())) {
            return;
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(response.getSchemaJson(), new TypeReference<>() {});
            List<Map<String, Object>> fields = asFieldList(schema.get("fields"));
            Map<String, Object> permissions = asMap(node == null ? null : node.getConfig().get("fieldPermissions"));
            List<Map<String, Object>> visibleFields = new ArrayList<>();
            for (Map<String, Object> field : fields) {
                String fieldKey = text(firstNonNull(field.get("fieldKey"), field.get("key")));
                String permission = text(permissions.getOrDefault(fieldKey, "EDITABLE"));
                if ("HIDDEN".equalsIgnoreCase(permission)) {
                    continue;
                }
                field.put("permission", permission);
                field.put("readOnly", readOnly || "READONLY".equalsIgnoreCase(permission));
                visibleFields.add(field);
            }
            schema.put("fields", visibleFields);
            response.setSchemaJson(objectMapper.writeValueAsString(schema));
        } catch (Exception exception) {
            throw new IllegalArgumentException("运行时表单字段权限解析失败", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asFieldList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Object firstNonNull(Object first, Object second) {
        return first == null ? second : first;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private WorkflowDefinition getDefinition(Long definitionId) {
        return definitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("流程定义不存在：" + definitionId));
    }

    private WorkflowDefinition getExecutableDefinition(WorkflowInstance instance) {
        WorkflowDefinition definition = getDefinition(instance.getDefinitionId());
        if (instance.getDefinitionVersionId() == null) {
            return getExecutableDefinition(definition);
        }
        return workflowVersionRepository.findById(instance.getDefinitionVersionId())
                .map(version -> snapshotDefinition(definition, version))
                .orElseGet(() -> getExecutableDefinition(definition));
    }

    private WorkflowDefinition getExecutableDefinition(WorkflowDefinition definition) {
        if (definition.getPublishedVersionId() != null) {
            return workflowVersionRepository.findById(definition.getPublishedVersionId())
                    .map(version -> snapshotDefinition(definition, version))
                    .orElse(definition);
        }
        return workflowVersionRepository.findTopByDefinitionIdOrderByVersionDesc(definition.getId())
                .map(version -> snapshotDefinition(definition, version))
                .orElse(definition);
    }

    private WorkflowDefinition snapshotDefinition(
            WorkflowDefinition definition,
            WorkflowVersion version
    ) {
        WorkflowDefinition snapshot = new WorkflowDefinition();
        snapshot.setId(definition.getId());
        snapshot.setCode(version.getCode());
        snapshot.setName(version.getName());
        snapshot.setDescription(version.getDescription());
        snapshot.setVersion(version.getVersion());
        snapshot.setPublishedVersionId(version.getId());
        snapshot.setFormJson(version.getFormJson());
        snapshot.setDefinitionJson(version.getDefinitionJson());
        snapshot.setBpmnXml(version.getBpmnXml());
        snapshot.setStatus(version.getStatus());
        snapshot.setEnabled(true);
        snapshot.setPublishedAt(version.getPublishedAt());
        return snapshot;
    }

    private WorkflowInstance getInstance(Long instanceId) {
        return instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("流程实例不存在：" + instanceId));
    }

    private Map<String, Object> parseBusinessData(String businessDataJson) {
        if (!StringUtils.hasText(businessDataJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(businessDataJson, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalArgumentException("业务数据JSON解析失败", exception);
        }
    }
}
