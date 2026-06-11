package com.example.metrics.util;

import com.example.metrics.model.entity.MetricConfig;
import lombok.Getter;

/**
 * SQL构建工具
 *
 * <p>根据指标配置构建SQL模板参数，供MyBatis动态SQL使用：
 * <pre>
 * SELECT {group_by}, {aggregation}({field}) as value
 * FROM {source_table}
 * [WHERE {filter_condition}]
 * [GROUP BY {group_by}]
 * [ORDER BY {sort_rule}]
 * </pre>
 *
 * <p>配置校验在创建指标时已完成，此处仅负责组装参数。
 *
 * @see MetricConfig
 * @see FieldValidator
 */
public class SqlBuilder {

    /**
     * SQL模板参数，用于MyBatis动态SQL
     */
    @Getter
    public static class SqlTemplate {
        private final String sourceTable;
        private final String field;
        private final String aggregation;
        private final String groupBy;
        private final String filterCondition;
        private final String sortRule;

        public SqlTemplate(String sourceTable, String field, String aggregation,
                           String groupBy, String filterCondition, String sortRule) {
            this.sourceTable = sourceTable;
            this.field = field;
            this.aggregation = aggregation;
            this.groupBy = groupBy;
            this.filterCondition = filterCondition;
            this.sortRule = sortRule;
        }
    }

    /**
     * 根据指标配置构建SQL模板参数
     *
     * <p>配置已在创建时通过FieldValidator校验，此处仅组装参数。
     */
    public static SqlTemplate buildSqlTemplate(MetricConfig config) {
        String sortRule = (config.getSortRule() != null && !config.getSortRule().isEmpty())
                ? config.getSortRule()
                : "value DESC";

        return new SqlTemplate(
                config.getSourceTable(),
                config.getField(),
                config.getAggregation(),
                config.getGroupBy(),
                config.getFilterCondition(),
                sortRule
        );
    }
}
