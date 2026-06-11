package com.example.metrics.exception;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * <p>定义系统使用的错误码：
 * <ul>
 *   <li>4xx: 客户端错误（参数问题、配置问题）</li>
 *   <li>5xx: 服务端错误（数据库、消息队列、系统异常）</li>
 * </ul>
 *
 * @see BusinessException
 * @see GlobalExceptionHandler
 */
@Getter
public enum ErrorCode {
    // 4xx 客户端错误
    PARAM_VALIDATION_FAILED(40001, "参数校验失败"),
    METRIC_NOT_FOUND(40002, "指标配置不存在"),
    METRIC_ALREADY_EXISTS(40003, "指标配置已存在"),
    METRIC_CONFIG_INVALID(40004, "指标配置不合法（字段/表不存在）"),
    AGGREGATION_FIELD_MISMATCH(40005, "聚合方式与字段类型不匹配"),
    TASK_NOT_FOUND(40006, "任务不存在"),
    TASK_STILL_RUNNING(40007, "任务仍在执行中"),
    DUPLICATE_TASK_SUBMISSION(40008, "重复提交任务"),

    // 5xx 服务端错误
    DATABASE_ERROR(50001, "数据库执行异常"),
    MESSAGE_QUEUE_ERROR(50002, "消息队列异常"),
    SYSTEM_ERROR(50003, "系统内部异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
