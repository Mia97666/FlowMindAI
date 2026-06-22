package com.flowmind.workflow.repository;

import com.flowmind.workflow.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流版本数据访问。
 */
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, Long> {

    /**
     * 查询某个流程定义下的版本列表，版本号倒序。
     */
    List<WorkflowVersion> findByDefinitionIdOrderByVersionDesc(Long definitionId);

    /**
     * 查询某个流程定义下的最新版本。
     */
    Optional<WorkflowVersion> findTopByDefinitionIdOrderByVersionDesc(Long definitionId);
}
