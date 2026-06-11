package com.example.metrics.service;

import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import com.example.metrics.mapper.MetricConfigMapper;
import com.example.metrics.model.dto.request.CreateMetricConfigRequest;
import com.example.metrics.model.dto.request.UpdateMetricConfigRequest;
import com.example.metrics.model.dto.response.MetricConfigResponse;
import com.example.metrics.model.entity.MetricConfig;
import com.example.metrics.util.FieldValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标配置业务逻辑服务
 *
 * <p>负责指标配置的CRUD操作和业务校验：
 * <ul>
 *   <li>创建指标配置：校验字段、聚合方式、筛选条件合法性</li>
 *   <li>更新指标配置：支持部分更新，使用乐观锁防止并发冲突</li>
 *   <li>查询指标配置：支持分页、启停状态筛选、关键词搜索</li>
 *   <li>启用/停用指标配置：软启停，不删除数据</li>
 *   <li>校验指标配置：验证配置能否正常生成SQL</li>
 * </ul>
 *
 * <p>配置化设计：新增指标只需插入配置记录，无需编写新代码。
 *
 * @see com.example.metrics.mapper.MetricConfigMapper
 * @see com.example.metrics.model.entity.MetricConfig
 * @see com.example.metrics.util.FieldValidator
 */
@Service
public class MetricConfigService {

    private static final Logger log = LoggerFactory.getLogger(MetricConfigService.class);

    private final MetricConfigMapper metricConfigMapper;

    public MetricConfigService(MetricConfigMapper metricConfigMapper) {
        this.metricConfigMapper = metricConfigMapper;
    }

    /**
     * 创建指标配置
     *
     * <p>校验指标名称唯一性、字段合法性、聚合方式与字段类型匹配度、
     * 筛选条件安全性（SQL注入防护），然后保存配置。
     *
     * @param request 创建请求
     * @return 创建成功的配置
     * @throws BusinessException 配置名称已存在或校验失败
     */
    @Transactional
    public MetricConfigResponse create(CreateMetricConfigRequest request) {
        log.info("Creating metric config: {}", request.getMetricName());

        MetricConfig existing = metricConfigMapper.selectByName(request.getMetricName());
        if (existing != null) {
            throw new BusinessException(ErrorCode.METRIC_ALREADY_EXISTS,
                    "指标名称 '" + request.getMetricName() + "' 已存在");
        }

        // 校验配置合法性（表名、字段、聚合方式、聚合与字段匹配、筛选条件）
        FieldValidator.validateAll(request.getSourceTable(), request.getField(),
                request.getAggregation(), request.getFilterCondition());

        MetricConfig config = new MetricConfig();
        config.setMetricName(request.getMetricName());
        config.setDescription(request.getDescription());
        config.setSourceTable(request.getSourceTable());
        config.setField(request.getField());
        config.setAggregation(request.getAggregation().toUpperCase());
        config.setGroupBy(request.getGroupBy());
        config.setFilterCondition(request.getFilterCondition());
        config.setSortRule(request.getSortRule() != null ? request.getSortRule() : "value DESC");
        config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        Long insertedId = metricConfigMapper.insert(config);
        if (insertedId == null || insertedId <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "指标配置创建失败");
        }

        log.info("Metric config created successfully, id={}", config.getId());
        return toResponse(config);
    }

    /**
     * 更新指标配置
     *
     * <p>使用乐观锁（version字段）防止并发更新冲突。
     * 支持部分更新，只更新传入的非空字段。
     *
     * @param id      配置ID
     * @param request 更新内容
     * @return 更新后的配置
     */
    @Transactional
    public MetricConfigResponse update(Long id, UpdateMetricConfigRequest request) {
        log.info("Updating metric config: id={}", id);

        MetricConfig config = metricConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + id);
        }

        if (request.getMetricName() != null && !request.getMetricName().equals(config.getMetricName())) {
            MetricConfig existing = metricConfigMapper.selectByName(request.getMetricName());
            if (existing != null) {
                throw new BusinessException(ErrorCode.METRIC_ALREADY_EXISTS,
                        "指标名称 '" + request.getMetricName() + "' 已存在");
            }
            config.setMetricName(request.getMetricName());
        }

        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        // 部分更新校验：只校验传入的非 null 字段，并检查聚合与字段匹配性
        if (request.getSourceTable() != null || request.getField() != null ||
                request.getAggregation() != null || request.getFilterCondition() != null) {
            FieldValidator.validateForUpdate(request.getSourceTable(), request.getField(),
                    request.getAggregation(), request.getFilterCondition(),
                    new FieldValidator.ValidationContext(config.getSourceTable(),
                            config.getField(), config.getAggregation(), config.getFilterCondition()
                    )
            );
        }
        if (request.getSourceTable() != null) {
            config.setSourceTable(request.getSourceTable());
        }
        if (request.getField() != null) {
            config.setField(request.getField());
        }
        if (request.getAggregation() != null) {
            config.setAggregation(request.getAggregation().toUpperCase());
        }
        if (request.getGroupBy() != null) {
            config.setGroupBy(request.getGroupBy());
        }
        if (request.getFilterCondition() != null) {
            config.setFilterCondition(request.getFilterCondition());
        }
        if (request.getSortRule() != null) {
            config.setSortRule(request.getSortRule());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }

        int updated = metricConfigMapper.updateById(config);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，可能是版本冲突");
        }

        log.info("Metric config updated successfully, id={}", id);
        return toResponse(config);
    }

    /**
     * 根据ID查询指标配置
     *
     * @param id 配置ID
     * @return 配置详情
     */
    public MetricConfigResponse getById(Long id) {
        MetricConfig config = metricConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + id);
        }
        return toResponse(config);
    }

    /**
     * 根据ID查询指标配置实体（内部使用）
     *
     * @param id 配置ID
     * @return 配置实体
     */
    public MetricConfig getEntityById(Long id) {
        MetricConfig config = metricConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + id);
        }
        return config;
    }

    /**
     * 分页查询指标配置列表
     *
     * @param enabled  启停状态筛选（可选）
     * @param keyword  关键词搜索（可选，搜索名称和描述）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 配置列表
     */
    public List<MetricConfigResponse> list(Boolean enabled, String keyword, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<MetricConfig> configs = metricConfigMapper.selectByPage(offset, pageSize, enabled, keyword);
        return configs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 统计符合条件的配置数量
     *
     * @param enabled 启停状态筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @return 数量
     */
    public long count(Boolean enabled, String keyword) {
        return metricConfigMapper.countByCondition(enabled, keyword);
    }

    /**
     * 启用/停用指标配置
     *
     * <p>软启停操作，不删除配置数据。使用乐观锁防止并发冲突。
     *
     * @param id      配置ID
     * @param enabled 目标状态
     */
    @Transactional
    public void updateEnabled(Long id, Boolean enabled) {
        log.info("Updating metric config enabled: id={}, enabled={}", id, enabled);

        MetricConfig config = metricConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + id);
        }

        int updated = metricConfigMapper.updateEnabled(id, enabled, config.getVersion());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，可能是版本冲突");
        }

        log.info("Metric config enabled updated successfully, id={}", id);
    }

    /**
     * 删除指标配置
     *
     * @param id 配置ID
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting metric config: id={}", id);

        MetricConfig config = metricConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + id);
        }

        int deleted = metricConfigMapper.deleteById(id);
        if (deleted == 0) {
            log.warn("Metric config not found for deletion, id={}", id);
        }
        log.info("Metric config deleted, id={}", id);
    }

    /**
     * 校验指标配置合法性
     *
     * <p>在任务执行前调用，验证配置能否正常生成SQL。
     * 包括：表名、字段、聚合方式、筛选条件校验。
     *
     * @param id 配置ID
     */
    public void validateConfig(Long id) {
        MetricConfig config = metricConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.METRIC_NOT_FOUND, "id: " + id);
        }

        FieldValidator.validateAll(config.getSourceTable(), config.getField(),
                config.getAggregation(), config.getFilterCondition());
    }

    private MetricConfigResponse toResponse(MetricConfig config) {
        MetricConfigResponse response = new MetricConfigResponse();
        response.setId(config.getId());
        response.setMetricName(config.getMetricName());
        response.setDescription(config.getDescription());
        response.setSourceTable(config.getSourceTable());
        response.setField(config.getField());
        response.setAggregation(config.getAggregation());
        response.setGroupBy(config.getGroupBy());
        response.setFilterCondition(config.getFilterCondition());
        response.setSortRule(config.getSortRule());
        response.setEnabled(config.getEnabled());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }
}