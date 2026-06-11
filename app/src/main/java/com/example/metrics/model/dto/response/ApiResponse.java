package com.example.metrics.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一API响应格式
 *
 * <p>所有API接口统一使用此格式返回响应：
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {...},
 *   "timestamp": "2026-06-05T10:00:00",
 *   "traceId": "uuid-xxx",
 *   "details": "..."
 * }
 * </pre>
 *
 * <p>code说明：
 * <ul>
 *   <li>2xx: 成功</li>
 *   <li>4xx: 客户端错误（参数校验、业务错误等）</li>
 *   <li>5xx: 服务端错误（系统异常）</li>
 * </ul>
 *
 * @param <T> data字段的类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private String timestamp;
    private String traceId;
    private String details;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now().format(FORMATTER);
    }

    public ApiResponse(int code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ApiResponse(int code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     响应类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message);
    }

    /**
     * 错误响应（带详情）
     *
     * @param code     错误码
     * @param message  错误信息
     * @param details  详细错误信息
     * @param traceId  追踪ID
     * @param <T>      响应类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message, String details, String traceId) {
        ApiResponse<T> response = new ApiResponse<>(code, message);
        response.setDetails(details);
        response.setTraceId(traceId);
        return response;
    }
}
