package com.example.metrics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.metrics.model.entity.Task;

import java.util.List;

/**
 * 任务数据访问层
 *
 * <p>提供任务数据的数据库操作：
 * <ul>
 *   <li>insert: 创建新任务</li>
 *   <li>updateById: 更新任务信息</li>
 *   <li>selectByTaskId: 根据taskId查询</li>
 *   <li>selectByMetricId: 根据指标ID查询任务列表</li>
 *   <li>selectByStatus: 根据状态查询任务</li>
 *   <li>selectByPage: 分页条件查询</li>
 *   <li>updateStatus: 更新任务状态</li>
 *   <li>updateResult: 更新任务结果</li>
 *   <li>selectPendingOrRunningByMetricId: 查询指标的pending/running任务</li>
 * </ul>
 *
 * @see com.example.metrics.model.entity.Task
 * @see com.example.metrics.model.enums.TaskStatus
 */
@Mapper
public interface TaskMapper {

    /**
     * 创建新任务
     *
     * @param task 任务实体
     * @return 自增主键ID
     */
    Long insert(Task task);

    /**
     * 更新任务信息
     *
     * <p>包括状态、开始/结束时间、结果数据等。
     *
     * @param task 任务实体
     * @return 影响行数
     */
    int updateById(Task task);

    /**
     * 根据taskId查询任务
     *
     * @param taskId 任务ID（UUID）
     * @return 任务详情
     */
    Task selectByTaskId(@Param("taskId") String taskId);

    /**
     * 根据指标ID查询任务列表
     *
     * @param metricId 指标配置ID
     * @return 任务列表，按创建时间倒序
     */
    List<Task> selectByMetricId(@Param("metricId") Long metricId);

    /**
     * 根据状态查询任务列表
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<Task> selectByStatus(@Param("status") String status);

    /**
     * 分页条件查询
     *
     * @param offset   偏移量
     * @param limit    返回数量
     * @param status   状态筛选（可选）
     * @param metricId 指标ID筛选（可选）
     * @return 任务列表
     */
    List<Task> selectByPage(@Param("offset") int offset, @Param("limit") int limit,
                            @Param("status") String status, @Param("metricId") Long metricId);

    /**
     * 条件统计数量
     *
     * @param status   状态筛选（可选）
     * @param metricId 指标ID筛选（可选）
     * @return 符合条件的数量
     */
    long countByCondition(@Param("status") String status, @Param("metricId") Long metricId);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    /**
     * 更新任务执行结果
     *
     * <p>在任务完成（成功或失败）时调用，记录结果数据和错误信息。
     *
     * @param taskId       任务ID
     * @param status       最终状态
     * @param resultData   结果数据（JSON格式）
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    int updateResult(@Param("taskId") String taskId, @Param("status") String status,
                     @Param("resultData") String resultData, @Param("errorMessage") String errorMessage);

    /**
     * 查询指标的pending/running任务
     *
     * <p>用于幂等性检查，防止重复创建任务。
     *
     * @param metricId 指标配置ID
     * @return pending或running状态的任务，不存在返回null
     */
    Task selectPendingOrRunningByMetricId(@Param("metricId") Long metricId);
}
