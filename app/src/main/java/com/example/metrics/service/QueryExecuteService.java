package com.example.metrics.service;

import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import com.example.metrics.mapper.MetricConfigMapper;
import com.example.metrics.model.entity.MetricConfig;
import com.example.metrics.util.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 查询执行服务
 *
 * <p>负责根据指标配置动态生成SQL并执行查询：
 * <ul>
 *   <li>根据指标配置生成查询SQL</li>
 *   <li>执行SQL查询并限制结果集大小</li>
 * </ul>
 *
 * <p>使用配置驱动设计，通过SqlBuilder根据metric_config表中的配置
 * 动态拼接SQL，无需为每个指标编写独立查询逻辑。
 *
 * @see com.example.metrics.model.entity.MetricConfig
 * @see com.example.metrics.util.SqlBuilder
 */
@Service
public class QueryExecuteService {

    private static final Logger log = LoggerFactory.getLogger(QueryExecuteService.class);

    /** 单次查询最大返回条数，防止内存溢出 */
    private static final int MAX_RESULT_SIZE = 1000;

    private final MetricConfigMapper metricConfigMapper;
    private final MetricConfigService metricConfigService;

    public QueryExecuteService(MetricConfigMapper metricConfigMapper, MetricConfigService metricConfigService) {
        this.metricConfigMapper = metricConfigMapper;
        this.metricConfigService = metricConfigService;
    }

    /**
     * 执行指标查询
     *
     * <p>根据指标配置生成SQL并执行：
     * <ol>
     *   <li>获取指标配置</li>
     *   <li>校验配置是否启用</li>
     *   <li>生成SQL语句</li>
     *   <li>执行查询</li>
     *   <li>限制结果集大小</li>
     * </ol>
     *
     * @param metricId 指标配置ID
     * @return 查询结果列表，每行数据以Map形式存储
     */
    public List<Map<String, Object>> executeQuery(Long metricId) {
        log.info("Executing query for metricId={}", metricId);

        MetricConfig config = metricConfigService.getEntityById(metricId);

        if (!config.getEnabled()) {
            throw new BusinessException(ErrorCode.METRIC_CONFIG_INVALID,
                    "指标配置已停用");
        }

        SqlBuilder.SqlTemplate template = SqlBuilder.buildSqlTemplate(config);
        log.info("Built SQL template for metricId={}", metricId);

        List<Map<String, Object>> result = metricConfigMapper.executeMetricQuery(template);

        if (result.size() > MAX_RESULT_SIZE) {
            log.warn("Result size {} exceeds limit {}, truncating", result.size(), MAX_RESULT_SIZE);
            result = result.subList(0, MAX_RESULT_SIZE);
        }

        log.info("Query executed successfully, metricId={}, resultSize={}", metricId, result.size());
        return result;
    }
}

