package com.example.metrics.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * <p>用于处理业务逻辑中的异常情况：
 * <ul>
 *   <li>指标配置不存在</li>
 *   <li>指标配置已存在</li>
 *   <li>任务不存在</li>
 *   <li>配置校验失败</li>
 * </ul>
 *
 * <p>使用ErrorCode枚举定义错误类型，便于统一管理和API返回。
 *
 * @see ErrorCode
 * @see GlobalExceptionHandler
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String details) {
        super(errorCode.getMessage() + ": " + details);
        this.errorCode = errorCode;
        this.details = details;
    }

    public int getCode() {
        return errorCode.getCode();
    }

    @Override
    public String getMessage() {
        return errorCode.getMessage();
    }
}
