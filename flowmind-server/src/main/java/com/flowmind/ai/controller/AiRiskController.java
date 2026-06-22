package com.flowmind.ai.controller;

import com.flowmind.ai.controller.dto.AiRiskCheckRequest;
import com.flowmind.workflow.dto.AiRiskCheckResult;
import com.flowmind.workflow.service.AiRiskCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 能力接口。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRiskController {

    private final AiRiskCheckService aiRiskCheckService;

    /**
     * 单独执行风险检测。
     *
     * 该接口便于前端在流程设计或表单提交前测试风险评分效果。
     */
    @PostMapping("/risk-check")
    public AiRiskCheckResult riskCheck(@RequestBody AiRiskCheckRequest request) {
        return aiRiskCheckService.check(
                request.getWorkflowCode(),
                request.getBusinessData()
        );
    }
}
