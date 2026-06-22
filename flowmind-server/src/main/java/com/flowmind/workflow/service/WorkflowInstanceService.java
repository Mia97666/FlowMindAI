package com.flowmind.workflow.service;

import com.flowmind.workflow.dto.StartWorkflowRequest;
import com.flowmind.workflow.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 工作流实例服务。
 *
 * 负责启动流程、查询流程实例。
 */
public interface WorkflowInstanceService {

    /**
     * 启动一个流程实例。
     *
     * @param definitionId 流程定义ID
     * @param request 启动流程请求
     * @return 新创建的流程实例
     */
    WorkflowInstance start(Long definitionId, StartWorkflowRequest request);

    /**
     * 查询所有流程实例。
     */
    List<WorkflowInstance> list(String starter);

    /**
     * 分页查询流程实例。
     */
    Page<WorkflowInstance> queryPage(
            String starter,
            String title,
            Long definitionId,
            String status,
            String riskLevel,
            Pageable pageable
    );

    /**
     * 查询单个流程实例。
     */
    WorkflowInstance get(Long id);
}
