package com.flowmind.workflow.risk;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地规则执行结果。
 */
@Data
public class RiskRuleEvaluation {

    private Integer riskScore = 0;

    private String suggestion;

    private List<RiskRuleMatch> matches = new ArrayList<>();

    public boolean hasMatches() {
        return matches != null && !matches.isEmpty();
    }
}
