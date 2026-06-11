package com.example.metrics.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标配置响应DTO
 *
 * <p>用于API接口返回指标配置的详细信息。
 */
@Data
public class MetricConfigResponse {

    private Long id;
    private String metricName;
    private String description;
    private String sourceTable;
    private String field;
    private String aggregation;
    private String groupBy;
    private String filterCondition;
    private String sortRule;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
