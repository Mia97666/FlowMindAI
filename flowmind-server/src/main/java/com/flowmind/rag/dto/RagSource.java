package com.flowmind.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RagSource {

    /**
     * 文件id
     */
    private Long documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 分片id
     */
    private Long chunkId;

    /**
     * 下标
     */
    private Integer chunkIndex;

    /**
     * 分
     */
    private Double score;

    /**
     * 内容
     */
    private String content;
}