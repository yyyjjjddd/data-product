package com.example.metrics.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新状态请求DTO
 *
 * <p>用于PATCH /api/v1/metrics/configs/{id}/enabled接口的请求参数。
 *
 * @see com.example.metrics.controller.MetricConfigController#updateEnabled
 */
@Data
public class UpdateStatusRequest {
    @NotNull(message = "enabled参数不能为空")
    private Boolean enabled;
}

