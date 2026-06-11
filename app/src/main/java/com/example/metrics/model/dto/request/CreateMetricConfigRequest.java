package com.example.metrics.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建指标配置请求DTO
 *
 * <p>用于POST /api/v1/metrics/configs接口的请求参数。
 *
 * @see com.example.metrics.controller.MetricConfigController#create
 */
@Data
public class CreateMetricConfigRequest {

    @NotBlank(message = "指标名称不能为空")
    @Size(min = 1, max = 100, message = "指标名称长度必须在1-100之间")
    private String metricName;

    @Size(max = 500, message = "指标描述长度不能超过500")
    private String description;

    @NotBlank(message = "来源数据表不能为空")
    private String sourceTable;

    @NotBlank(message = "统计字段不能为空")
    private String field;

    @NotBlank(message = "聚合方式不能为空")
    private String aggregation;

    private String groupBy;

    private String filterCondition;

    private String sortRule;

    private Boolean enabled = true;
}
