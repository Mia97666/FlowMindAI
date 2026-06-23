package com.flowmind.workflow.risk;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 规则命中明细，写入风险原因和审计上下文。
 */
@Data
@AllArgsConstructor
public class RiskRuleMatch {

    private String ruleId;

    private String expression;

    private Integer score;

    private String reason;
}
