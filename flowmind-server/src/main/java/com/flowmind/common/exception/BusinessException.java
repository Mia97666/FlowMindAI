package com.flowmind.common.exception;

/**
 * 业务异常基类。
 *
 * 所有业务异常应继承此类，全局异常处理器会统一处理。
 * 子类通过 HTTP 状态码区分不同的错误类型。
 *
 * code 为业务错误码，用于前端区分错误类型，与 httpStatus 解耦。
 * 未显式指定时，默认与 httpStatus 一致，保持向后兼容。
 */
public class BusinessException extends RuntimeException {

    private final int httpStatus;

    private final int code;

    public BusinessException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = httpStatus;
    }

    public BusinessException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.code = httpStatus;
    }

    /**
     * 通过 ErrorCode 构造，code 与 httpStatus 来自 ErrorCode。
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
    }

    /**
     * 通过 ErrorCode 构造，使用自定义异常内容覆盖默认 message。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
    }

    /**
     * 通过 ErrorCode 构造，携带原始异常，便于保留堆栈。
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
    }

    /**
     * 通过 ErrorCode 构造，携带原始异常，message 使用 ErrorCode 默认描述。
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.httpStatus = errorCode.getHttpStatus().value();
        this.code = errorCode.getCode();
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }
}
