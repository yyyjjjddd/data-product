package com.example.metrics.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务响应DTO
 *
 * <p>用于API接口返回任务的详细信息，包括执行状态和结果。
 */
@Data
public class TaskResponse {

    private String taskId;
    private Long metricId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Object resultData;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
}
