package com.flowmind.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次 RAG 请求的管线选项。
 *
 * 该对象只影响本次请求，不修改系统全局配置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagPipelineOptions {

    /**
     * 检索模式。
     *
     * LIGHT：轻量模式，强制关闭查询重写和多查询扩展。
     * HIGH_RECALL：高召回率模式，允许按开关启用增强步骤。
     */
    private String retrievalMode;

    /**
     * 是否启用查询重写。
     *
     * 只有 retrievalMode=HIGH_RECALL 时才会生效。
     */
    private Boolean queryRewriteEnabled;

    /**
     * 是否启用多查询扩展。
     *
     * 只有 retrievalMode=HIGH_RECALL 时才会生效。
     */
    private Boolean multiQueryEnabled;
}
