package com.flowmind.agent.state;

import com.flowmind.rag.dto.RagResponse;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * LangGraph 工作流状态对象。
 *
 * 作用类似传统工作流里的流程变量。
 * 所有节点都通过这个 State 读取输入、写入输出。
 */
@Data
public class ApprovalAgentState {

    /**
     * 用户原始问题或审批请求。
     */
    private String question;

    /**
     * RAG 返回结果。
     */
    private RagResponse ragResponse;

    /**
     * 后续可以扩展：
     * requestType、riskScore、approvalDecision 等。
     */
    private Map<String, Object> variables = new HashMap<>();
}
