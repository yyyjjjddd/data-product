package com.example.metrics.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建任务请求DTO
 *
 * <p>用于POST /api/v1/tasks接口的请求参数。
 *
 * @see com.example.metrics.controller.TaskController#create
 */
@Data
public class CreateTaskRequest {

    @NotNull(message = "指标ID不能为空")
    private Long metricId;
}
