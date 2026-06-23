package com.flowmind.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自研 RAG 管线开关。
 *
 * 默认优先可用性和响应速度；需要更高召回率时，可按环境打开改写和多查询。
 */
@Data
@Component
@ConfigurationProperties(prefix = "flowmind.rag.pipeline")
public class RagPipelineProperties {

    private int defaultTopK = 8;

    private double defaultMinScore = 0.4D;

    private boolean queryRewriteEnabled = false;

    private boolean multiQueryEnabled = false;

    private int maxMultiQueries = 3;

    private boolean keywordSearchEnabled = true;

    private boolean contextCompressionEnabled = true;

    private boolean rerankEnabled = true;

    private boolean timingLogEnabled = true;

    private boolean responseCacheEnabled = true;

    private int responseCacheMaxSize = 128;

    private long responseCacheTtlSeconds = 300;
}
