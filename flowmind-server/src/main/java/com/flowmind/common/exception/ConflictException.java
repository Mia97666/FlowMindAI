package com.flowmind.common.exception;

/**
 * 资源冲突异常（如重复提交、状态不允许），对应 HTTP 409。
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(409, message);
    }
}