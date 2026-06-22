package com.flowmind.agent.service;

import com.flowmind.agent.state.ApprovalAgentState;
import com.flowmind.rag.dto.RagResponse;
import com.flowmind.rag.service.impl.RagPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * FlowMind AI Agent 工作流服务。
 *
 * 当前版本：
 * 先把 RAG Pipeline 包装成一个 Agent 节点。
 *
 * 后续会演进为 LangGraph4j StateGraph：
 * Start → Classify → RAG → RiskEvaluate → ApprovalDecision → End
 */
@Service
@RequiredArgsConstructor
public class AgentWorkflowService {

    private final RagPipelineService ragPipelineService;

    /**
     * 执行最小 Agent 工作流。
     *
     * @param question 用户问题
     * @return 工作流最终状态
     */
    public ApprovalAgentState run(String question){
        ApprovalAgentState state = new ApprovalAgentState();

        // 1. 写入初始输入
        state.setQuestion(question);

        // 2. RAG 节点：调用已有 RAG Pipeline
        RagResponse ragResponse = ragPipelineService.ask(question);

        // 3. 写入节点输出
        state.setRagResponse(ragResponse);

        return state;
    }
}
