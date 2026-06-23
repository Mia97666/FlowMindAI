package com.flowmind.workflow.risk;

import lombok.Data;

/**
 * 单条风险规则。
 */
@Data
public class RiskRule {

    private String id;

    private String expression;

    private Integer score = 0;

    private String reason;

    private String suggestion;

    private Boolean stopProcessing = false;
}
