package com.example.metrics.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新指标配置请求DTO
 *
 * <p>用于PUT /api/v1/metrics/configs/{id}接口的请求参数。
 * 所有字段可选，只更新传入的非空字段。
 *
 * @see com.example.metrics.controller.MetricConfigController#update
 */
@Data
public class UpdateMetricConfigRequest {

    @Size(min = 1, max = 100, message = "指标名称长度必须在1-100之间")
    private String metricName;

    @Size(max = 500, message = "指标描述长度不能超过500")
    private String description;

    private String sourceTable;
    private String field;
    private String aggregation;
    private String groupBy;
    private String filterCondition;
    private String sortRule;
    private Boolean enabled;
}
