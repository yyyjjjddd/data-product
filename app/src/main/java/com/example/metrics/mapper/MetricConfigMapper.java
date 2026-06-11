package com.example.metrics.mapper;

import com.example.metrics.model.entity.MetricConfig;
import com.example.metrics.util.SqlBuilder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 指标配置数据访问层
 *
 * <p>提供指标配置数据的数据库操作：
 * <ul>
 *   <li>insert: 新增指标配置</li>
 *   <li>updateById: 更新指标配置（乐观锁）</li>
 *   <li>selectById: 根据ID查询</li>
 *   <li>selectByName: 根据名称查询</li>
 *   <li>selectAll: 查询所有配置</li>
 *   <li>selectByPage: 分页条件查询</li>
 *   <li>countByCondition: 条件统计数量</li>
 *   <li>updateEnabled: 启停状态更新（乐观锁）</li>
 *   <li>deleteById: 删除配置</li>
 * </ul>
 *
 * <p>使用乐观锁（version字段）防止并发更新冲突。
 *
 * @see com.example.metrics.model.entity.MetricConfig
 */
@Mapper
public interface MetricConfigMapper {

    /**
     * 新增指标配置
     *
     * @param config 配置实体
     * @return 自增主键ID
     */
    Long insert(MetricConfig config);

    /**
     * 更新指标配置
     *
     * <p>使用乐观锁，通过version字段防止并发冲突。
     * 如果版本不匹配，返回0表示更新失败。
     *
     * @param config 配置实体
     * @return 影响行数
     */
    int updateById(MetricConfig config);

    /**
     * 更新版本号
     *
     * <p>乐观锁实现，在更新配置时自动递增版本号。
     *
     * @param id      配置ID
     * @param version 当前版本号
     * @return 影响行数
     */
    int updateVersion(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 根据ID查询
     *
     * @param id 配置ID
     * @return 配置详情，不存在返回null
     */
    MetricConfig selectById(@Param("id") Long id);

    /**
     * 根据名称查询
     *
     * @param metricName 配置名称
     * @return 配置详情，用于校验名称唯一性
     */
    MetricConfig selectByName(@Param("metricName") String metricName);

    /**
     * 查询所有配置
     *
     * @return 配置列表，按创建时间倒序
     */
    List<MetricConfig> selectAll();

    /**
     * 分页条件查询
     *
     * @param offset   偏移量
     * @param limit    返回数量
     * @param enabled  启停状态筛选（可选）
     * @param keyword  关键词搜索（可选，搜索名称和描述）
     * @return 配置列表
     */
    List<MetricConfig> selectByPage(@Param("offset") int offset, @Param("limit") int limit,
                                    @Param("enabled") Boolean enabled, @Param("keyword") String keyword);

    /**
     * 条件统计数量
     *
     * @param enabled 启停状态筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @return 符合条件的数量
     */
    long countByCondition(@Param("enabled") Boolean enabled, @Param("keyword") String keyword);

    /**
     * 更新启停状态
     *
     * <p>使用乐观锁，通过version字段防止并发冲突。
     *
     * @param id      配置ID
     * @param enabled 目标状态
     * @param version 当前版本号
     * @return 影响行数，0表示版本冲突
     */
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled, @Param("version") Integer version);

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据指标配置执行动态查询
     *
     * @param template SQL模板参数
     * @return 查询结果列表
     */
    List<Map<String, Object>> executeMetricQuery(@Param("template") SqlBuilder.SqlTemplate template);
}
