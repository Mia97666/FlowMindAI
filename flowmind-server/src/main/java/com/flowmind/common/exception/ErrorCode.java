package com.flowmind.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。
 *
 * 统一管理系统中各类业务异常的：
 * 1. code：业务错误码，用于前端区分错误类型，与 HTTP 状态码解耦
 * 2. httpStatus：对应 HTTP 状态码
 * 3. message：默认异常描述
 *
 * 约定：
 * - 4xx：客户端错误（参数、权限等）
 * - 5xx：服务端错误，其中 50xxx 段用于外部依赖（如大模型、向量库）相关异常
 */
public enum ErrorCode {

    // ==================== 通用 ====================
    INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),

    // ==================== RAG / 大模型相关（50xxx） ====================
    /**
     * 调用向量模型失败，缺少 API Key。
     */
    EMBEDDING_API_KEY_MISSING(50001, HttpStatus.INTERNAL_SERVER_ERROR, "调用向量模型失败，缺少 API Key"),

    /**
     * 混合检索失败，缺少 API Key。
     */
    HYBRID_RETRIEVE_API_KEY_MISSING(50002, HttpStatus.INTERNAL_SERVER_ERROR, "混合检索失败，缺少 API Key"),

    /**
     * RAG 回答生成失败。
     */
    RAG_GENERATE_FAILED(50003, HttpStatus.INTERNAL_SERVER_ERROR, "RAG 回答生成失败"),

    /**
     * RAG 调用次数超出当日上限。
     */
    RAG_RATE_LIMIT_EXCEEDED(50004, HttpStatus.TOO_MANY_REQUESTS, "今日 RAG 调用次数已达上限");

    private final int code;

    private final HttpStatus httpStatus;

    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
