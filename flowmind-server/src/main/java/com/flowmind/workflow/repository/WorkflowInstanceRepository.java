package com.flowmind.workflow.repository;

import com.flowmind.workflow.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 工作流实例 Repository。
 *
 * 负责 workflow_instance 表的增删改查。
 */
public interface WorkflowInstanceRepository
        extends JpaRepository<WorkflowInstance, Long>, JpaSpecificationExecutor<WorkflowInstance> {

    /**
     * 根据流程定义ID查询所有实例。
     *
     * 例如：
     * 查询所有"采购审批流程"的历史申请。
     */
    List<WorkflowInstance> findByDefinitionIdOrderByCreatedAtDesc(Long definitionId);

    /**
     * 根据发起人查询实例。
     *
     * 例如：
     * 查询张三发起过哪些审批。
     */
    List<WorkflowInstance> findByStarterOrderByCreatedAtDesc(String starter);
}
