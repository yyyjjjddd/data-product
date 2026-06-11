package com.example.metrics.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 指标配置实体
 *
 * <p>对应数据库中的metric_config表，存储指标的配置信息。
 * 通过配置化设计，业务人员可以自行配置指标，无需编写代码。
 *
 * <p>配置字段说明：
 * <ul>
 *   <li>metric_name: 指标名称（唯一）</li>
 *   <li>description: 指标描述</li>
 *   <li>source_table: 来源数据表（当前固定为asset）</li>
 *   <li>field: 统计字段</li>
 *   <li>aggregation: 聚合方式（COUNT/SUM/AVG/MAX/MIN）</li>
 *   <li>group_by: 分组维度字段</li>
 *   <li>filter_condition: 固定筛选条件（SQL片段）</li>
 *   <li>sort_rule: 排序规则</li>
 *   <li>enabled: 启用状态</li>
 *   <li>version: 版本号（乐观锁）</li>
 * </ul>
 *
 * <p>通过SqlBuilder根据配置动态生成SQL，实现配置化查询。
 *
 * @see com.example.metrics.util.SqlBuilder
 */
@Data
@NoArgsConstructor
public class MetricConfig {

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
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
