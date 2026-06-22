package com.flowmind.workflow.engine.support;

import com.flowmind.workflow.entity.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件表达式计算器。
 *
 * MVP 阶段不引入复杂脚本引擎，避免审批条件执行任意代码。
 * 当前支持常见比较表达式：
 * riskScore >= 70
 * riskLevel == "HIGH"
 * amount < 200000
 * businessData.amount >= 50000
 */
@Component
@RequiredArgsConstructor
public class ConditionEvaluator {

    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile(
            "^([a-zA-Z0-9_.]+)\\s*(>=|<=|==|!=|>|<)\\s*(.+)$"
    );

    /**
     * 判断表达式是否为默认分支。
     */
    public boolean isDefaultCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        String normalized = condition.trim();
        return "default".equalsIgnoreCase(normalized)
                || "else".equalsIgnoreCase(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "默认".equals(normalized);
    }

    /**
     * 计算条件表达式。
     */
    public boolean evaluate(
            String condition,
            WorkflowInstance instance,
            Map<String, Object> businessData
    ) {
        if (isDefaultCondition(condition)) {
            return true;
        }

        Matcher matcher = SIMPLE_EXPRESSION.matcher(condition.trim());
        if (!matcher.matches()) {
            return false;
        }

        Object leftValue = buildVariables(instance, businessData).get(matcher.group(1));
        String operator = matcher.group(2);
        String rightRaw = cleanValue(matcher.group(3));

        if (leftValue == null) {
            return false;
        }

        if (leftValue instanceof Number) {
            try {
                return compareNumber(new BigDecimal(leftValue.toString()), operator, new BigDecimal(rightRaw));
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return compareText(String.valueOf(leftValue), operator, rightRaw);
    }

    /**
     * 构建表达式变量表。
     */
    private Map<String, Object> buildVariables(
            WorkflowInstance instance,
            Map<String, Object> businessData
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("riskScore", instance.getRiskScore());
        variables.put("riskLevel", instance.getRiskLevel());
        variables.put("starter", instance.getStarter());

        for (Map.Entry<String, Object> entry : businessData.entrySet()) {
            variables.put(entry.getKey(), entry.getValue());
            variables.put("businessData." + entry.getKey(), entry.getValue());
        }
        return variables;
    }

    private String cleanValue(String value) {
        String cleaned = value.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            return cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    private boolean compareNumber(BigDecimal left, String operator, BigDecimal right) {
        int result = left.compareTo(right);
        return switch (operator) {
            case ">=" -> result >= 0;
            case "<=" -> result <= 0;
            case ">" -> result > 0;
            case "<" -> result < 0;
            case "==" -> result == 0;
            case "!=" -> result != 0;
            default -> false;
        };
    }

    private boolean compareText(String left, String operator, String right) {
        return switch (operator) {
            case "==" -> left.equalsIgnoreCase(right);
            case "!=" -> !left.equalsIgnoreCase(right);
            default -> false;
        };
    }
}
