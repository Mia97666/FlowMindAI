package com.flowmind.workflow.risk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 安全受限的本地规则引擎。
 *
 * 只支持白名单表达式，不执行脚本代码。
 */
@Slf4j
@Component
public class RiskRuleEngine {

    private static final Pattern COMPARISON = Pattern.compile(
            "^([a-zA-Z0-9_.]+)\\s*(not\\s+contains|contains|not\\s+in|in|>=|<=|==|!=|>|<)\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    public RiskRuleEvaluation evaluate(AiRiskStrategyConfig strategy, Map<String, Object> businessData) {
        RiskRuleEvaluation evaluation = new RiskRuleEvaluation();
        if (strategy == null || strategy.getRules() == null || strategy.getRules().isEmpty()) {
            return evaluation;
        }

        Map<String, Object> variables = buildVariables(strategy, businessData);
        int score = 0;
        for (RiskRule rule : strategy.getRules()) {
            if (rule == null || isBlank(rule.getExpression())) {
                continue;
            }
            boolean matched = evaluateExpression(rule.getExpression(), variables);
            if (!matched) {
                continue;
            }
            int ruleScore = rule.getScore() == null ? 0 : rule.getScore();
            score += ruleScore;
            evaluation.getMatches().add(new RiskRuleMatch(
                    rule.getId(),
                    rule.getExpression(),
                    ruleScore,
                    rule.getReason()
            ));
            if (!isBlank(rule.getSuggestion())) {
                evaluation.setSuggestion(rule.getSuggestion());
            }
            if (Boolean.TRUE.equals(rule.getStopProcessing())) {
                break;
            }
        }
        evaluation.setRiskScore(Math.min(100, Math.max(0, score)));
        return evaluation;
    }

    private boolean evaluateExpression(String expression, Map<String, Object> variables) {
        String trimmed = expression == null ? "" : expression.trim();
        if (trimmed.isBlank()) {
            return false;
        }

        List<String> orParts = splitOutsideQuotes(trimmed, "||");
        if (orParts.size() > 1) {
            return orParts.stream().anyMatch(part -> evaluateExpression(part, variables));
        }

        List<String> andParts = splitOutsideQuotes(trimmed, "&&");
        if (andParts.size() > 1) {
            return andParts.stream().allMatch(part -> evaluateExpression(part, variables));
        }

        return evaluateSingleCondition(trimmed, variables);
    }

    private boolean evaluateSingleCondition(String condition, Map<String, Object> variables) {
        Matcher matcher = COMPARISON.matcher(condition);
        if (!matcher.matches()) {
            log.debug("Unsupported risk rule expression: {}", condition);
            return false;
        }
        Object leftValue = variables.get(matcher.group(1));
        if (leftValue == null) {
            return false;
        }

        String operator = matcher.group(2).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String rightRaw = cleanValue(matcher.group(3));
        Object rightValue = variables.containsKey(rightRaw) ? variables.get(rightRaw) : rightRaw;

        if (isNumeric(leftValue) && isNumeric(rightValue)) {
            BigDecimal left = toDecimal(leftValue);
            BigDecimal right = toDecimal(rightValue);
            return compareNumber(left, operator, right);
        }
        return compareText(String.valueOf(leftValue), operator, String.valueOf(rightValue));
    }

    private Map<String, Object> buildVariables(AiRiskStrategyConfig strategy, Map<String, Object> businessData) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (businessData == null) {
            return variables;
        }
        for (Map.Entry<String, Object> entry : businessData.entrySet()) {
            variables.put(entry.getKey(), entry.getValue());
            variables.put("businessData." + entry.getKey(), entry.getValue());
        }
        applyFieldAliases(strategy, variables);
        return variables;
    }

    private void applyFieldAliases(AiRiskStrategyConfig strategy, Map<String, Object> variables) {
        if (strategy == null || strategy.getFieldAliases() == null || strategy.getFieldAliases().isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : strategy.getFieldAliases().entrySet()) {
            String target = entry.getKey();
            if (isBlank(target) || hasValue(variables.get(target))) {
                continue;
            }
            for (String source : entry.getValue()) {
                Object value = variables.get(source);
                if (!hasValue(value)) {
                    value = variables.get("businessData." + source);
                }
                if (hasValue(value)) {
                    variables.put(target, value);
                    variables.put("businessData." + target, value);
                    break;
                }
            }
        }
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
        String leftValue = left == null ? "" : left;
        String rightValue = right == null ? "" : right;
        return switch (operator) {
            case "==" -> leftValue.equalsIgnoreCase(rightValue);
            case "!=" -> !leftValue.equalsIgnoreCase(rightValue);
            case "contains" -> leftValue.contains(rightValue);
            case "not contains" -> !leftValue.contains(rightValue);
            case "in" -> splitList(rightValue).stream().anyMatch(item -> leftValue.equalsIgnoreCase(item));
            case "not in" -> splitList(rightValue).stream().noneMatch(item -> leftValue.equalsIgnoreCase(item));
            default -> false;
        };
    }

    private List<String> splitOutsideQuotes(String value, String delimiter) {
        List<String> parts = new ArrayList<>();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int start = 0;
        for (int i = 0; i <= value.length() - delimiter.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (!inSingleQuote && !inDoubleQuote && value.startsWith(delimiter, i)) {
                parts.add(value.substring(start, i).trim());
                start = i + delimiter.length();
                i = start - 1;
            }
        }
        if (start == 0) {
            return List.of(value);
        }
        parts.add(value.substring(start).trim());
        return parts;
    }

    private List<String> splitList(String value) {
        String cleaned = value.trim();
        if ((cleaned.startsWith("[") && cleaned.endsWith("]"))
                || (cleaned.startsWith("(") && cleaned.endsWith(")"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.isBlank()) {
            return List.of();
        }
        return List.of(cleaned.split(","))
                .stream()
                .map(this::cleanValue)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String cleanValue(String value) {
        String cleaned = value == null ? "" : value.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            return cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }

    private boolean isNumeric(Object value) {
        if (value == null) {
            return false;
        }
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BigDecimal toDecimal(Object value) {
        return new BigDecimal(String.valueOf(value));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }
}
