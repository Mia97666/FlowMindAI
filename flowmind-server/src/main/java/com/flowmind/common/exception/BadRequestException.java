package com.flowmind.common.exception;

/**
 * 请求参数非法异常，对应 HTTP 400。
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super(400, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(400, message, cause);
    }
}