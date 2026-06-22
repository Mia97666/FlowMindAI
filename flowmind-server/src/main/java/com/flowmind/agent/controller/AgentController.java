package com.flowmind.agent.controller;

import com.flowmind.agent.service.AgentWorkflowService;
import com.flowmind.agent.state.ApprovalAgentState;
import com.flowmind.rag.dto.RagRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 工作流接口。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentWorkflowService agentWorkflowService;

    /**
     * 执行 Agent 工作流。
     *
     * @param request 用户问题或审批请求
     * @return Agent 执行后的完整状态
     */
    @PostMapping("/run")
    public ApprovalAgentState run(@RequestBody RagRequest request){
        return agentWorkflowService.run(request.getQuestion());
    }
}
