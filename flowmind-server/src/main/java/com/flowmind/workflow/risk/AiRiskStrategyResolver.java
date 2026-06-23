package com.flowmind.workflow.risk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.workflow.dto.AiRiskExecutionRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 策略解析器。
 *
 * 当前从 classpath JSON 加载，后续可平滑替换为数据库 Repository。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRiskStrategyResolver {

    private static final String DEFAULT_STRATEGY_CODE = "DEFAULT_AMOUNT_RISK";

    private final ObjectMapper objectMapper;

    private final Map<String, AiRiskStrategyConfig> strategies = new LinkedHashMap<>();

    @PostConstruct
    public void loadStrategies() {
        List<AiRiskStrategyConfig> loaded = readConfiguredStrategies();
        if (loaded.isEmpty()) {
            loaded = List.of(defaultStrategy());
        }
        strategies.clear();
        for (AiRiskStrategyConfig strategy : loaded) {
            if (Boolean.FALSE.equals(strategy.getEnabled()) || isBlank(strategy.getStrategyCode())) {
                continue;
            }
            register(strategy.getStrategyCode(), strategy);
            register(strategy.getStrategyName(), strategy);
            for (String alias : strategy.getAliases()) {
                register(alias, strategy);
            }
        }
        register(DEFAULT_STRATEGY_CODE, strategies.get(normalize(DEFAULT_STRATEGY_CODE)));
        log.info("Loaded {} AI risk strategy keys", strategies.size());
    }

    public AiRiskStrategyConfig resolve(AiRiskExecutionRequest request) {
        Map<String, Object> config = request == null || request.getNodeConfig() == null
                ? Map.of()
                : request.getNodeConfig();
        for (String candidate : strategyCandidates(config)) {
            AiRiskStrategyConfig strategy = strategies.get(normalize(candidate));
            if (strategy != null) {
                return mergeNodeOverrides(strategy, config);
            }
        }
        AiRiskStrategyConfig fallback = strategies.get(normalize(DEFAULT_STRATEGY_CODE));
        return mergeNodeOverrides(fallback != null ? fallback : defaultStrategy(), config);
    }

    private List<AiRiskStrategyConfig> readConfiguredStrategies() {
        ClassPathResource resource = new ClassPathResource("ai-risk-strategies.json");
        if (!resource.exists()) {
            return List.of();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to load AI risk strategies, fallback to default strategy: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> strategyCandidates(Map<String, Object> config) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, config.get("aiStrategyCode"));
        addCandidate(candidates, config.get("strategyCode"));
        addCandidate(candidates, config.get("aiStrategy"));
        addCandidate(candidates, config.get("riskScenario"));
        return candidates;
    }

    private AiRiskStrategyConfig mergeNodeOverrides(AiRiskStrategyConfig strategy, Map<String, Object> config) {
        AiRiskStrategyConfig copy = objectMapper.convertValue(strategy, AiRiskStrategyConfig.class);
        copy.setThreshold(readInt(config.get("threshold"), copy.getThreshold()));
        copy.setAutoApproveMaxScore(readInt(config.get("autoApproveMaxScore"), copy.getAutoApproveMaxScore()));
        copy.setTopK(readInt(config.get("topK"), copy.getTopK()));
        copy.setMinScore(readDouble(config.get("minScore"), copy.getMinScore()));
        if (!isBlank(config.get("ragScope"))) {
            copy.setRagScope(String.valueOf(config.get("ragScope")));
        }
        if (!isBlank(config.get("prompt"))) {
            copy.setPromptTemplate(String.valueOf(config.get("prompt")));
        }
        return copy;
    }

    private void register(String key, AiRiskStrategyConfig strategy) {
        if (!isBlank(key) && strategy != null) {
            strategies.put(normalize(key), strategy);
        }
    }

    private void addCandidate(List<String> candidates, Object value) {
        if (!isBlank(value)) {
            candidates.add(String.valueOf(value));
        }
    }

    private int readInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue == null ? 0 : defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue == null ? 0 : defaultValue;
        }
    }

    private double readDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue == null ? 0.4D : defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue == null ? 0.4D : defaultValue;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private AiRiskStrategyConfig defaultStrategy() {
        AiRiskStrategyConfig strategy = new AiRiskStrategyConfig();
        strategy.setStrategyCode(DEFAULT_STRATEGY_CODE);
        strategy.setStrategyName("通用金额风险策略");
        strategy.setAliases(List.of("DEFAULT", "通用风险评分策略"));
        strategy.setDefaultSuggestion("建议结合金额、业务必要性和制度依据进入后续审批。");

        RiskRule highAmount = new RiskRule();
        highAmount.setId("amount-high");
        highAmount.setExpression("amount >= 200000");
        highAmount.setScore(85);
        highAmount.setReason("申请金额较高，达到通用高风险阈值。");
        highAmount.setSuggestion("建议转财务负责人或更高级审批人复核。");

        RiskRule mediumAmount = new RiskRule();
        mediumAmount.setId("amount-medium");
        mediumAmount.setExpression("amount >= 50000");
        mediumAmount.setScore(55);
        mediumAmount.setReason("申请金额达到通用中风险阈值。");
        mediumAmount.setSuggestion("建议部门负责人或财务人员关注金额合理性。");

        strategy.setRules(List.of(highAmount, mediumAmount));
        return strategy;
    }
}
