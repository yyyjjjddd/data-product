package com.example.metrics.service;

import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import com.example.metrics.mapper.MetricConfigMapper;
import com.example.metrics.model.entity.MetricConfig;
import com.example.metrics.util.SqlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QueryExecuteService单元测试
 *
 * <p>测试查询执行服务：
 * <ul>
 *   <li>正常情况：执行查询并返回结果</li>
 *   <li>异常情况：配置不存在、配置停用</li>
 *   <li>边界条件：结果集大小限制</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class QueryExecuteServiceTest {

    @Mock
    private MetricConfigMapper metricConfigMapper;

    @Mock
    private MetricConfigService metricConfigService;

    private QueryExecuteService queryExecuteService;

    @BeforeEach
    void setUp() {
        queryExecuteService = new QueryExecuteService(metricConfigMapper, metricConfigService);
    }

    // ========== executeQuery 测试 ==========

    @Test
    void executeQuery_withValidConfig_shouldReturnResults() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, true);
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(createResultRow("approved", 10));
        mockResults.add(createResultRow("rejected", 5));
        when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                .thenReturn(mockResults);

        // when
        List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

        // then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(metricConfigMapper).executeMetricQuery(any(SqlBuilder.SqlTemplate.class));
    }

    @Test
    void executeQuery_withDisabledConfig_shouldThrowException() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, false);
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                queryExecuteService.executeQuery(metricId)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("已停用"));
    }

    @Test
    void executeQuery_withConfigNotFound_shouldThrowException() {
        // given
        Long metricId = 999L;
        when(metricConfigService.getEntityById(metricId)).thenThrow(
                new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + metricId)
        );

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                queryExecuteService.executeQuery(metricId)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void executeQuery_withResultExceedsLimit_shouldTruncateResults() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, true);
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);

        // 创建超过1000条的结果
        List<Map<String, Object>> mockResults = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            mockResults.add(createResultRow("status_" + i, i));
        }
        when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                .thenReturn(mockResults);

        // when
        List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

        // then
        assertEquals(1000, results.size()); // 应该被截断到1000条
    }

    @Test
    void executeQuery_withEmptyResults_shouldReturnEmptyList() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, true);
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);
        when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                .thenReturn(new ArrayList<>());

        // when
        List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void executeQuery_withAllAggregationTypes_shouldWork() {
        // given
        String[] aggregations = {"COUNT", "SUM", "AVG", "MAX", "MIN"};

        for (String agg : aggregations) {
            Long metricId = 1L;
            MetricConfig config = createMetricConfig(metricId, true);
            config.setAggregation(agg);
            when(metricConfigService.getEntityById(metricId)).thenReturn(config);

            List<Map<String, Object>> mockResults = new ArrayList<>();
            mockResults.add(createResultRow("group1", 100));
            when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                    .thenReturn(mockResults);

            // when
            List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

            // then
            assertNotNull(results, "Aggregation " + agg + " should return results");
        }
    }

    @Test
    void executeQuery_withFilterCondition_shouldPassToMapper() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, true);
        config.setFilterCondition("status='approved'");
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(createResultRow("上海", 15));
        when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                .thenReturn(mockResults);

        // when
        List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

        // then
        assertNotNull(results);
        verify(metricConfigMapper).executeMetricQuery(argThat(template ->
                "status='approved'".equals(template.getFilterCondition())));
    }

    @Test
    void executeQuery_atExactLimit_shouldNotTruncate() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, true);
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);

        // 创建正好1000条的结果
        List<Map<String, Object>> mockResults = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            mockResults.add(createResultRow("status_" + i, i));
        }
        when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                .thenReturn(mockResults);

        // when
        List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

        // then
        assertEquals(1000, results.size()); // 不应该被截断
    }

    @Test
    void executeQuery_atLimitPlusOne_shouldTruncate() {
        // given
        Long metricId = 1L;
        MetricConfig config = createMetricConfig(metricId, true);
        when(metricConfigService.getEntityById(metricId)).thenReturn(config);

        // 创建1001条结果
        List<Map<String, Object>> mockResults = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            mockResults.add(createResultRow("status_" + i, i));
        }
        when(metricConfigMapper.executeMetricQuery(any(SqlBuilder.SqlTemplate.class)))
                .thenReturn(mockResults);

        // when
        List<Map<String, Object>> results = queryExecuteService.executeQuery(metricId);

        // then
        assertEquals(1000, results.size()); // 应该被截断
    }

    // ========== 辅助方法 ==========

    private MetricConfig createMetricConfig(Long id, boolean enabled) {
        MetricConfig config = new MetricConfig();
        config.setId(id);
        config.setMetricName("test_metric");
        config.setSourceTable("asset");
        config.setField("asset_id");
        config.setAggregation("COUNT");
        config.setGroupBy("status");
        config.setFilterCondition(null);
        config.setSortRule("value DESC");
        config.setEnabled(enabled);
        config.setVersion(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private Map<String, Object> createResultRow(String groupByValue, Object aggregatedValue) {
        Map<String, Object> row = new HashMap<>();
        row.put("status", groupByValue);
        row.put("value", aggregatedValue);
        return row;
    }
}
