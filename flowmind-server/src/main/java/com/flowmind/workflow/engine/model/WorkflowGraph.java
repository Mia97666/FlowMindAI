package com.flowmind.workflow.engine.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端流程设计器保存下来的流程图。
 *
 * nodes 表示节点，edges 表示连线。执行器只依赖这两个核心集合，
 * 因此前端无论使用 Vue Flow 还是 LogicFlow，只要最终转换成该结构即可执行。
 */
@Data
public class WorkflowGraph {

    /**
     * 流程节点集合。
     */
    private List<WorkflowNode> nodes = new ArrayList<>();

    /**
     * 流程连线集合。
     */
    private List<WorkflowEdge> edges = new ArrayList<>();
}
