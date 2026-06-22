package com.flowmind.common.config;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.flowmind.common.dto.ApiResponse;
import com.flowmind.common.exception.BusinessException;
import com.flowmind.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 统一将各类异常转换为 ApiResponse 格式返回，
 * 避免前端收到非结构化错误信息。
 *
 * 业务异常返回 {code: 业务错误码, message: 异常内容, data: null}，
 * 其中 code 与 HTTP 状态码解耦，前端可据 code 区分错误类型。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常 [code={}, http={}]: {}", e.getCode(), e.getHttpStatus(), e.getMessage());
        ApiResponse<Void> body = ApiResponse.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    /**
     * 缺少大模型 API Key 兜底处理。
     *
     * 正常情况下调用方会捕获 NoApiKeyException 并转换为
     * 携带具体 ErrorCode 的 BusinessException；
     * 这里作为全局兜底，避免该异常以 500 形式泄露堆栈。
     */
    @ExceptionHandler(NoApiKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoApiKey(NoApiKeyException e) {
        log.warn("缺少 API Key: {}", e.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                "调用外部模型服务失败，缺少 API Key"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ApiResponse.error(400, "请求体格式错误，请检查JSON格式");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMessage());
        return ApiResponse.error(405, "不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突: {}", e.getMessage());
        return ApiResponse.error(409, "数据冲突，请检查输入是否重复");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
        log.warn("业务状态异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        log.error("服务异常: {}", e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}