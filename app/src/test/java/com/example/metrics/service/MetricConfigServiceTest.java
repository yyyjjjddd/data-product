package com.example.metrics.service;

import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import com.example.metrics.mapper.MetricConfigMapper;
import com.example.metrics.model.dto.request.CreateMetricConfigRequest;
import com.example.metrics.model.dto.request.UpdateMetricConfigRequest;
import com.example.metrics.model.dto.response.MetricConfigResponse;
import com.example.metrics.model.entity.MetricConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MetricConfigService单元测试
 *
 * <p>测试指标配置业务逻辑：
 * <ul>
 *   <li>正常情况：创建、更新、查询、删除配置</li>
 *   <li>异常情况：配置不存在、名称重复、校验失败</li>
 *   <li>边界条件：分页参数、null值处理</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MetricConfigServiceTest {

    @Mock
    private MetricConfigMapper metricConfigMapper;

    private MetricConfigService metricConfigService;

    @BeforeEach
    void setUp() {
        metricConfigService = new MetricConfigService(metricConfigMapper);
    }

    // ========== create 测试 ==========

    @Test
    void create_withValidRequest_shouldCreateConfig() {
        // given
        CreateMetricConfigRequest request = createValidRequest();
        when(metricConfigMapper.selectByName(request.getMetricName())).thenReturn(null);
        when(metricConfigMapper.insert(any(MetricConfig.class))).thenReturn(1L);

        // when
        MetricConfigResponse response = metricConfigService.create(request);

        // then
        assertNotNull(response);
        assertEquals(request.getMetricName(), response.getMetricName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getSourceTable(), response.getSourceTable());
        assertEquals(request.getField(), response.getField());
        assertEquals("COUNT", response.getAggregation()); // 转为大写
        verify(metricConfigMapper).insert(any(MetricConfig.class));
    }

    @Test
    void create_withDuplicateName_shouldThrowException() {
        // given
        CreateMetricConfigRequest request = createValidRequest();
        MetricConfig existingConfig = new MetricConfig();
        existingConfig.setMetricName(request.getMetricName());
        when(metricConfigMapper.selectByName(request.getMetricName())).thenReturn(existingConfig);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.create(request)
        );
        assertEquals(ErrorCode.METRIC_ALREADY_EXISTS, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("已存在"));
    }

    @Test
    void create_withInvalidField_shouldThrowException() {
        // given
        CreateMetricConfigRequest request = createValidRequest();
        request.setField("invalid_field");
        when(metricConfigMapper.selectByName(request.getMetricName())).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.create(request)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
    }

    @Test
    void create_withAggregationFieldMismatch_shouldThrowException() {
        // given
        CreateMetricConfigRequest request = createValidRequest();
        request.setField("status"); // 非数值字段
        request.setAggregation("SUM"); // SUM只能用于数值字段
        when(metricConfigMapper.selectByName(request.getMetricName())).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.create(request)
        );
        assertEquals(ErrorCode.AGGREGATION_FIELD_MISMATCH, exception.getErrorCode());
    }

    @Test
    void create_withDefaultEnabled_shouldSetEnabledTrue() {
        // given
        CreateMetricConfigRequest request = createValidRequest();
        request.setEnabled(null);
        when(metricConfigMapper.selectByName(request.getMetricName())).thenReturn(null);
        when(metricConfigMapper.insert(any(MetricConfig.class))).thenAnswer(invocation -> {
            MetricConfig config = invocation.getArgument(0);
            assertTrue(config.getEnabled());
            return 1L;
        });

        // when
        MetricConfigResponse response = metricConfigService.create(request);

        // then
        assertTrue(response.getEnabled());
    }

    @Test
    void create_withDefaultSortRule_shouldSetDefaultValue() {
        // given
        CreateMetricConfigRequest request = createValidRequest();
        request.setSortRule(null);
        when(metricConfigMapper.selectByName(request.getMetricName())).thenReturn(null);
        when(metricConfigMapper.insert(any(MetricConfig.class))).thenAnswer(invocation -> {
            MetricConfig config = invocation.getArgument(0);
            assertEquals("value DESC", config.getSortRule());
            return 1L;
        });

        // when
        metricConfigService.create(request);

        // then
        verify(metricConfigMapper).insert(any(MetricConfig.class));
    }

    // ========== update 测试 ==========

    @Test
    void update_withValidRequest_shouldUpdateConfig() {
        // given
        Long id = 1L;
        UpdateMetricConfigRequest request = new UpdateMetricConfigRequest();
        request.setDescription("新描述");

        MetricConfig existingConfig = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(existingConfig);
        when(metricConfigMapper.updateById(any(MetricConfig.class))).thenReturn(1);

        // when
        MetricConfigResponse response = metricConfigService.update(id, request);

        // then
        assertNotNull(response);
        assertEquals("新描述", response.getDescription());
    }

    @Test
    void update_withNonExistentId_shouldThrowException() {
        // given
        Long id = 999L;
        UpdateMetricConfigRequest request = new UpdateMetricConfigRequest();
        request.setDescription("新描述");
        when(metricConfigMapper.selectById(id)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.update(id, request)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void update_withNewNameAlreadyExists_shouldThrowException() {
        // given
        Long id = 1L;
        UpdateMetricConfigRequest request = new UpdateMetricConfigRequest();
        request.setMetricName("new_name");

        MetricConfig existingConfig = createExistingConfig(id);
        existingConfig.setMetricName("old_name");

        MetricConfig anotherConfig = new MetricConfig();
        anotherConfig.setMetricName("new_name");

        when(metricConfigMapper.selectById(id)).thenReturn(existingConfig);
        when(metricConfigMapper.selectByName("new_name")).thenReturn(anotherConfig);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.update(id, request)
        );
        assertEquals(ErrorCode.METRIC_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void update_withConcurrentModification_shouldThrowException() {
        // given
        Long id = 1L;
        UpdateMetricConfigRequest request = new UpdateMetricConfigRequest();
        request.setDescription("新描述");

        MetricConfig existingConfig = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(existingConfig);
        when(metricConfigMapper.updateById(any(MetricConfig.class))).thenReturn(0); // 乐观锁失败

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.update(id, request)
        );
        assertEquals(ErrorCode.SYSTEM_ERROR, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("版本冲突"));
    }

    // ========== getById 测试 ==========

    @Test
    void getById_withExistingId_shouldReturnConfig() {
        // given
        Long id = 1L;
        MetricConfig config = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(config);

        // when
        MetricConfigResponse response = metricConfigService.getById(id);

        // then
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("test_metric", response.getMetricName());
    }

    @Test
    void getById_withNonExistentId_shouldThrowException() {
        // given
        Long id = 999L;
        when(metricConfigMapper.selectById(id)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.getById(id)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    // ========== getEntityById 测试 ==========

    @Test
    void getEntityById_withExistingId_shouldReturnEntity() {
        // given
        Long id = 1L;
        MetricConfig config = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(config);

        // when
        MetricConfig result = metricConfigService.getEntityById(id);

        // then
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void getEntityById_withNonExistentId_shouldThrowException() {
        // given
        Long id = 999L;
        when(metricConfigMapper.selectById(id)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.getEntityById(id)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    // ========== list 测试 ==========

    @Test
    void list_withPagination_shouldReturnConfigs() {
        // given
        int pageNum = 1;
        int pageSize = 10;
        int offset = 0;
        List<MetricConfig> configs = Arrays.asList(createExistingConfig(1L), createExistingConfig(2L));
        when(metricConfigMapper.selectByPage(offset, pageSize, null, null)).thenReturn(configs);

        // when
        List<MetricConfigResponse> responses = metricConfigService.list(null, null, pageNum, pageSize);

        // then
        assertEquals(2, responses.size());
        verify(metricConfigMapper).selectByPage(0, 10, null, null);
    }

    @Test
    void list_withEnabledFilter_shouldFilterByEnabled() {
        // given
        int pageNum = 1;
        int pageSize = 10;
        Boolean enabled = true;
        List<MetricConfig> configs = Arrays.asList(createExistingConfig(1L));
        when(metricConfigMapper.selectByPage(0, 10, enabled, null)).thenReturn(configs);

        // when
        List<MetricConfigResponse> responses = metricConfigService.list(enabled, null, pageNum, pageSize);

        // then
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getEnabled());
    }

    @Test
    void list_withKeywordSearch_shouldSearchByKeyword() {
        // given
        int pageNum = 1;
        int pageSize = 10;
        String keyword = "test";
        List<MetricConfig> configs = Arrays.asList(createExistingConfig(1L));
        when(metricConfigMapper.selectByPage(0, 10, null, keyword)).thenReturn(configs);

        // when
        List<MetricConfigResponse> responses = metricConfigService.list(null, keyword, pageNum, pageSize);

        // then
        assertEquals(1, responses.size());
    }

    @Test
    void list_withLargePageNum_shouldCalculateCorrectOffset() {
        // given
        int pageNum = 5;
        int pageSize = 10;
        when(metricConfigMapper.selectByPage(anyInt(), anyInt(), any(), any())).thenReturn(List.of());

        // when
        metricConfigService.list(null, null, pageNum, pageSize);

        // then
        verify(metricConfigMapper).selectByPage(40, 10, null, null); // (5-1)*10 = 40
    }

    // ========== count 测试 ==========

    @Test
    void count_shouldReturnTotalCount() {
        // given
        when(metricConfigMapper.countByCondition(null, null)).thenReturn(100L);

        // when
        long count = metricConfigService.count(null, null);

        // then
        assertEquals(100L, count);
    }

    // ========== updateEnabled 测试 ==========

    @Test
    void updateEnabled_shouldUpdateStatus() {
        // given
        Long id = 1L;
        Boolean enabled = false;
        MetricConfig config = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(config);
        when(metricConfigMapper.updateEnabled(id, enabled, config.getVersion())).thenReturn(1);

        // when & then - 不抛异常即成功
        assertDoesNotThrow(() -> metricConfigService.updateEnabled(id, enabled));
    }

    @Test
    void updateEnabled_withNonExistentId_shouldThrowException() {
        // given
        Long id = 999L;
        when(metricConfigMapper.selectById(id)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.updateEnabled(id, false)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateEnabled_withConcurrentModification_shouldThrowException() {
        // given
        Long id = 1L;
        MetricConfig config = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(config);
        when(metricConfigMapper.updateEnabled(id, false, config.getVersion())).thenReturn(0);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.updateEnabled(id, false)
        );
        assertEquals(ErrorCode.SYSTEM_ERROR, exception.getErrorCode());
    }

    // ========== delete 测试 ==========

    @Test
    void delete_withExistingId_shouldDeleteConfig() {
        // given
        Long id = 1L;
        MetricConfig config = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(config);
        when(metricConfigMapper.deleteById(id)).thenReturn(1);

        // when & then - 不抛异常即成功
        assertDoesNotThrow(() -> metricConfigService.delete(id));
        verify(metricConfigMapper).deleteById(id);
    }

    @Test
    void delete_withNonExistentId_shouldThrowException() {
        // given
        Long id = 999L;
        when(metricConfigMapper.selectById(id)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.delete(id)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    // ========== validateConfig 测试 ==========

    @Test
    void validateConfig_withValidConfig_shouldPass() {
        // given
        Long id = 1L;
        MetricConfig config = createExistingConfig(id);
        when(metricConfigMapper.selectById(id)).thenReturn(config);

        // when & then - 不抛异常即成功
        assertDoesNotThrow(() -> metricConfigService.validateConfig(id));
    }

    @Test
    void validateConfig_withNonExistentId_shouldThrowException() {
        // given
        Long id = 999L;
        when(metricConfigMapper.selectById(id)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.validateConfig(id)
        );
        assertEquals(ErrorCode.METRIC_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void validateConfig_withInvalidConfig_shouldThrowException() {
        // given
        Long id = 1L;
        MetricConfig config = createExistingConfig(id);
        config.setField("invalid_field");
        when(metricConfigMapper.selectById(id)).thenReturn(config);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                metricConfigService.validateConfig(id)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
    }

    // ========== 辅助方法 ==========

    private CreateMetricConfigRequest createValidRequest() {
        CreateMetricConfigRequest request = new CreateMetricConfigRequest();
        request.setMetricName("test_metric");
        request.setDescription("测试指标");
        request.setSourceTable("asset");
        request.setField("asset_id");
        request.setAggregation("COUNT");
        request.setGroupBy("status");
        request.setFilterCondition(null);
        request.setSortRule("value DESC");
        request.setEnabled(true);
        return request;
    }

    private MetricConfig createExistingConfig(Long id) {
        MetricConfig config = new MetricConfig();
        config.setId(id);
        config.setMetricName("test_metric");
        config.setDescription("测试指标");
        config.setSourceTable("asset");
        config.setField("asset_id");
        config.setAggregation("COUNT");
        config.setGroupBy("status");
        config.setFilterCondition(null);
        config.setSortRule("value DESC");
        config.setEnabled(true);
        config.setVersion(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }
}
