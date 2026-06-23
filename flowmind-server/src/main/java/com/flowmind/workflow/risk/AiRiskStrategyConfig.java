package com.flowmind.workflow.risk;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 风险策略配置。
 *
 * 业务含义来自配置，Java 代码只负责解释和执行这些规则。
 */
@Data
public class AiRiskStrategyConfig {

    private String strategyCode;

    private String strategyName;

    private List<String> aliases = new ArrayList<>();

    private Boolean enabled = true;

    private Integer lowMaxScore = 30;

    private Integer highMinScore = 70;

    private Integer autoApproveMaxScore = 30;

    private Integer threshold = 70;

    private Integer topK = 8;

    private Double minScore = 0.4D;

    private String ragScope;

    /**
     * NEVER：只用本地规则；ON_RULE_MISS：未命中规则时调用模型；ALWAYS：规则后仍调用模型补充。
     */
    private String llmMode = "ON_RULE_MISS";

    private String promptTemplate;

    private String defaultSuggestion = "建议按当前风险等级进入后续审批路径。";

    /**
     * 语义字段别名映射。
     *
     * key 是规则表达式里的语义字段，value 是业务表单中可能出现的字段 key。
     */
    private Map<String, List<String>> fieldAliases = new LinkedHashMap<>();

    private List<RiskRule> rules = new ArrayList<>();
}
