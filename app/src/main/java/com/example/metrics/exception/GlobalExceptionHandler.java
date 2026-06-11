package com.example.metrics.exception;

import com.example.metrics.model.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一处理所有未捕获的异常，返回统一的错误响应格式：
 * <ul>
 *   <li>BusinessException: 业务异常，返回业务错误码和详情</li>
 *   <li>MethodArgumentNotValidException: 参数校验异常，返回400错误</li>
 *   <li>BindException: 参数绑定异常，返回400错误</li>
 *   <li>Exception: 其他未预期异常，返回500错误</li>
 * </ul>
 *
 * <p>使用@RestControllerAdvice实现全局异常处理，
 * 所有异常都被捕获并转换为ApiResponse格式返回。
 *
 * @see ApiResponse
 * @see BusinessException
 * @see ErrorCode
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     *
     * <p>业务异常包含错误码和详情，用于明确告知客户端错误原因。
     * 日志级别为WARN，避免大量业务异常充斥日志。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.warn("Business exception occurred, traceId={}, code={}, message={}, details={}",
                traceId, e.getCode(), e.getMessage(), e.getDetails());
        return ApiResponse.error(e.getCode(), e.getMessage(), e.getDetails(), traceId);
    }

    /**
     * 处理参数校验异常（@Valid校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation exception occurred, traceId={}, details={}", traceId, details);
        return ApiResponse.error(ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                ErrorCode.PARAM_VALIDATION_FAILED.getMessage(), details, traceId);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        String details = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Bind exception occurred, traceId={}, details={}", traceId, details);
        return ApiResponse.error(ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                ErrorCode.PARAM_VALIDATION_FAILED.getMessage(), details, traceId);
    }

    /**
     * 处理未预期的系统异常
     *
     * <p>将异常信息记录到ERROR日志，便于排查问题。
     * 响应内容不包含具体错误信息，防止泄露系统细节。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        log.error("Unexpected exception occurred, traceId={}", traceId, e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getMessage(), null, traceId);
    }

    private String getTraceId(HttpServletRequest request) {
        String traceId = (String) request.getAttribute("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
