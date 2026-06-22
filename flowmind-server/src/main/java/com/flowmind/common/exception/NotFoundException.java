package com.flowmind.common.exception;

/**
 * 资源不存在异常，对应 HTTP 404。
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(404, message);
    }

    public NotFoundException(String resource, Object id) {
        super(404, resource + "不存在：" + id);
    }
}