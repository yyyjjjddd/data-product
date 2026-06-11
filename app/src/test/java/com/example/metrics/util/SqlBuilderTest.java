package com.example.metrics.util;

import com.example.metrics.model.entity.MetricConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SqlBuilder单元测试
 *
 * <p>测试SQL模板构建逻辑：
 * <ul>
 *   <li>正常情况：各种配置的SQL模板生成</li>
 *   <li>边界条件：sortRule为null或空字符串</li>
 * </ul>
 */
class SqlBuilderTest {

    @Test
    void buildSqlTemplate_withAllFields_shouldBuildCorrectTemplate() {
        // given
        MetricConfig config = createMetricConfig(
                "test_metric",
                "asset",
                "asset_id",
                "COUNT",
                "status",
                "status='approved'",
                "value DESC"
        );

        // when
        SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);

        // then
        assertEquals("asset", template.getSourceTable());
        assertEquals("asset_id", template.getField());
        assertEquals("COUNT", template.getAggregation());
        assertEquals("status", template.getGroupBy());
        assertEquals("status='approved'", template.getFilterCondition());
        assertEquals("value DESC", template.getSortRule());
    }

    @Test
    void buildSqlTemplate_withNullSortRule_shouldUseDefault() {
        // given
        MetricConfig config = createMetricConfig(
                "test_metric",
                "asset",
                "file_size_bytes",
                "SUM",
                "uploader",
                null,
                null
        );

        // when
        SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);

        // then
        assertEquals("value DESC", template.getSortRule());
    }

    @Test
    void buildSqlTemplate_withEmptySortRule_shouldUseDefault() {
        // given
        MetricConfig config = createMetricConfig(
                "test_metric",
                "asset",
                "file_size_bytes",
                "AVG",
                "city",
                "status='pending'",
                ""
        );

        // when
        SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);

        // then
        assertEquals("value DESC", template.getSortRule());
    }

    @Test
    void buildSqlTemplate_withNullFilterCondition_shouldPassNull() {
        // given
        MetricConfig config = createMetricConfig(
                "test_metric",
                "asset",
                "asset_id",
                "COUNT",
                "platform",
                null,
                "value DESC"
        );

        // when
        SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);

        // then
        assertNull(template.getFilterCondition());
    }

    @Test
    void buildSqlTemplate_withAllAggregations_shouldWorkCorrectly() {
        // given - 测试所有聚合方式
        String[] aggregations = {"COUNT", "SUM", "AVG", "MAX", "MIN"};

        for (String agg : aggregations) {
            MetricConfig config = createMetricConfig(
                    "test_metric_" + agg,
                    "asset",
                    "file_size_bytes",
                    agg,
                    "uploader",
                    null,
                    "value DESC"
            );

            // when
            SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);

            // then
            assertEquals(agg, template.getAggregation(),
                    "Aggregation " + agg + " should be preserved");
        }
    }

    @Test
    void buildSqlTemplate_withDifferentGroupByFields_shouldWorkCorrectly() {
        // given - 测试不同的分组字段
        String[] groupByFields = {"status", "uploader", "city", "platform"};

        for (String groupBy : groupByFields) {
            MetricConfig config = createMetricConfig(
                    "test_metric_" + groupBy,
                    "asset",
                    "asset_id",
                    "COUNT",
                    groupBy,
                    null,
                    "value DESC"
            );

            // when
            SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);

            // then
            assertEquals(groupBy, template.getGroupBy(), "GroupBy " + groupBy + " should be preserved");
        }
    }

    private MetricConfig createMetricConfig(String metricName, String sourceTable,
                                            String field, String aggregation, String groupBy,
                                            String filterCondition, String sortRule) {
        MetricConfig config = new MetricConfig();
        config.setMetricName(metricName);
        config.setSourceTable(sourceTable);
        config.setField(field);
        config.setAggregation(aggregation);
        config.setGroupBy(groupBy);
        config.setFilterCondition(filterCondition);
        config.setSortRule(sortRule);
        return config;
    }
}
