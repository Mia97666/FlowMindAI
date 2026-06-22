package com.flowmind.workflow.repository;

import com.flowmind.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository
        extends JpaRepository<WorkflowDefinition, Long>, JpaSpecificationExecutor<WorkflowDefinition> {

    Optional<WorkflowDefinition> findByCode(String code);

    /**
     * 查询某个流程编码下的全部版本。
     */
    List<WorkflowDefinition> findByCodeOrderByVersionDesc(String code);

    /**
     * 查询某个流程编码下的最新版本。
     */
    Optional<WorkflowDefinition> findTopByCodeOrderByVersionDesc(String code);

    /**
     * 查询所有启用流程，供用户发起审批时选择。
     */
    List<WorkflowDefinition> findByEnabledTrueOrderByUpdatedAtDesc();
}
