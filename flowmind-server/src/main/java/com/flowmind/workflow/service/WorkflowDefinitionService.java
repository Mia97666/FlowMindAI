package com.flowmind.workflow.service;

import com.flowmind.workflow.dto.WorkflowDefinitionRequest;
import com.flowmind.workflow.dto.WorkflowPreCheckResult;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.entity.WorkflowVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WorkflowDefinitionService {

    WorkflowDefinition create(
            WorkflowDefinitionRequest request
    );

    WorkflowDefinition update(
            Long id,
            WorkflowDefinitionRequest request
    );

    List<WorkflowDefinition> list();

    List<WorkflowDefinition> listEnabled();

    Page<WorkflowDefinition> queryPage(
            String name,
            String code,
            String status,
            String category,
            Pageable pageable
    );

    WorkflowDefinition get(Long id);

    WorkflowPreCheckResult preCheck(Long id, WorkflowDefinitionRequest request);

    WorkflowDefinition publish(Long id);

    List<WorkflowVersion> listVersions(Long id);

    WorkflowDefinition enable(Long id);

    WorkflowDefinition disable(Long id);
}
