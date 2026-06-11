package com.example.metrics.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务实体
 *
 * <p>对应数据库中的task表，存储异步查询任务的信息
 *
 * @see com.example.metrics.model.enums.TaskStatus
 */
@Data
@NoArgsConstructor
public class Task {

    private Long id;
    private String taskId;
    private Long metricId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String resultData;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

