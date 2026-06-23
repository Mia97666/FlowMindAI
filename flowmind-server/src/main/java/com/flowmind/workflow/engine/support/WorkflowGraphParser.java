package com.flowmind.workflow.engine.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.workflow.engine.model.WorkflowEdge;
import com.flowmind.workflow.engine.model.WorkflowGraph;
import com.flowmind.workflow.engine.model.WorkflowNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义 JSON 解析器。
 *
 * 为了兼容 Vue Flow 的原始数据结构，该解析器会同时识别：
 * 1. 后端标准结构：nodes[].type / nodes[].name / nodes[].config
 * 2. Vue Flow 结构：nodes[].data.nodeType / nodes[].data.label / nodes[].data.config
 */
@Component
@RequiredArgsConstructor
public class WorkflowGraphParser {

    private final ObjectMapper objectMapper;

    /**
     * 解析流程图。
     */
    public WorkflowGraph parse(String definitionJson) {
        if (definitionJson == null || definitionJson.isBlank()) {
            return defaultGraph();
        }

        try {
            Map<String, Object> root = objectMapper.readValue(
                    definitionJson,
                    new TypeReference<>() {}
            );
            WorkflowGraph graph = new WorkflowGraph();
            graph.setNodes(parseNodes(asList(root.get("nodes"))));
            graph.setEdges(parseEdges(asList(root.get("edges"))));
            return graph;
        } catch (Exception e) {
            throw new IllegalArgumentException("流程定义JSON解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 找到开始节点。
     */
    public WorkflowNode findStartNode(WorkflowGraph graph) {
        return graph.getNodes().stream()
                .filter(node -> "START".equalsIgnoreCase(node.getType()))
                .findFirst()
                .orElseGet(() -> graph.getNodes().isEmpty() ? null : graph.getNodes().get(0));
    }

    /**
     * 根据ID查找节点。
     */
    public WorkflowNode findNode(WorkflowGraph graph, String nodeId) {
        return graph.getNodes().stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    private List<WorkflowNode> parseNodes(List<Map<String, Object>> rawNodes) {
        List<WorkflowNode> nodes = new ArrayList<>();
        for (Map<String, Object> rawNode : rawNodes) {
            Map<String, Object> data = asMap(rawNode.get("data"));

            WorkflowNode node = new WorkflowNode();
            node.setId(asText(rawNode.get("id")));
            node.setType(firstNotBlank(
                    asText(rawNode.get("nodeType")),
                    asText(data.get("nodeType")),
                    asText(data.get("type")),
                    asText(rawNode.get("type"))
            ));
            node.setName(firstNotBlank(
                    asText(rawNode.get("name")),
                    asText(rawNode.get("label")),
                    asText(data.get("name")),
                    asText(data.get("label"))
            ));
            node.setConfig(asMap(firstNonNull(rawNode.get("config"), data.get("config"))));

            if (node.getId() != null && node.getType() != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private List<WorkflowEdge> parseEdges(List<Map<String, Object>> rawEdges) {
        List<WorkflowEdge> edges = new ArrayList<>();
        for (Map<String, Object> rawEdge : rawEdges) {
            Map<String, Object> data = asMap(rawEdge.get("data"));

            WorkflowEdge edge = new WorkflowEdge();
            edge.setId(asText(rawEdge.get("id")));
            edge.setSource(asText(rawEdge.get("source")));
            edge.setTarget(asText(rawEdge.get("target")));
            String label = firstNotBlank(asText(rawEdge.get("label")), asText(data.get("label")));
            String condition = firstNotBlank(
                    asText(rawEdge.get("condition")),
                    asText(data.get("condition"))
            );
            edge.setCondition(condition != null ? condition : conditionFromLabel(label));
            edge.setLabel(label);

            if (edge.getSource() != null && edge.getTarget() != null) {
                edges.add(edge);
            }
        }
        return edges;
    }

    /**
     * 当管理员还没设计流程时，提供一个最小可运行流程，避免接口报空指针。
     */
    private WorkflowGraph defaultGraph() {
        WorkflowGraph graph = new WorkflowGraph();

        WorkflowNode start = new WorkflowNode();
        start.setId("start");
        start.setType("START");
        start.setName("开始");

        WorkflowNode end = new WorkflowNode();
        end.setId("end");
        end.setType("END");
        end.setName("结束");

        WorkflowEdge edge = new WorkflowEdge();
        edge.setId("edge_start_end");
        edge.setSource("start");
        edge.setTarget("end");

        graph.setNodes(List.of(start, end));
        graph.setEdges(List.of(edge));
        return graph;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
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
        return new LinkedHashMap<>();
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Object firstNonNull(Object first, Object second) {
        return first == null ? second : first;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String conditionFromLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.trim();
        if ("default".equalsIgnoreCase(normalized)
                || "else".equalsIgnoreCase(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "默认".equals(normalized)
                || normalized.matches("^[a-zA-Z0-9_.]+\\s*(>=|<=|==|!=|>|<)\\s*.+$")) {
            return normalized;
        }
        return null;
    }
}
