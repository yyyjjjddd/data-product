package com.example.metrics.service;

import com.example.metrics.config.RabbitMQConfig;
import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import com.example.metrics.mapper.TaskMapper;
import com.example.metrics.model.dto.request.CreateTaskRequest;
import com.example.metrics.model.dto.response.TaskResponse;
import com.example.metrics.model.entity.MetricConfig;
import com.example.metrics.model.entity.Task;
import com.example.metrics.model.enums.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务业务逻辑服务
 *
 * <p>负责异步任务的创建、状态管理和消息发送：
 * <ul>
 *   <li>创建查询任务：校验指标配置、发送消息到RabbitMQ</li>
 *   <li>查询任务状态：支持分页和状态筛选</li>
 *   <li>更新任务状态和结果</li>
 *   <li>重试次数管理</li>
 * </ul>
 *
 * <p>幂等性设计：相同指标ID的pending/running任务不会重复创建。
 *
 * @see com.example.metrics.mapper.TaskMapper
 * @see com.example.metrics.service.TaskConsumerService
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskMapper taskMapper;
    private final MetricConfigService metricConfigService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public TaskService(TaskMapper taskMapper, MetricConfigService metricConfigService,
                       RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.metricConfigService = metricConfigService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建查询任务
     *
     * <p>校验指标配置存在且启用后，创建任务记录并发送到RabbitMQ队列。
     * 支持幂等性：如果该指标存在pending/running状态的任务，直接返回原任务。
     *
     * @param request 包含metricId的请求
     * @return 创建的任务信息
     */
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        log.info("Creating task for metricId={}", request.getMetricId());

        MetricConfig config = metricConfigService.getEntityById(request.getMetricId());
        if (!config.getEnabled()) {
            throw new BusinessException(ErrorCode.METRIC_CONFIG_INVALID,
                    "指标配置已停用，无法创建任务");
        }

        Task existingTask = taskMapper.selectPendingOrRunningByMetricId(request.getMetricId());
        if (existingTask != null) {
            log.info("Found existing pending/running task: {}", existingTask.getTaskId());
            return toResponse(existingTask);
        }

        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setMetricId(request.getMetricId());
        task.setStatus(TaskStatus.PENDING.getValue());
        task.setRetryCount(0);

        Long insertedId = taskMapper.insert(task);
        if (insertedId == null || insertedId <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "任务创建失败");
        }

        sendTaskMessage(task);

        log.info("Task created successfully: taskId={}", task.getTaskId());
        return toResponse(task);
    }

    /**
     * 根据taskId查询任务
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    public TaskResponse getByTaskId(String taskId) {
        Task task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "taskId: " + taskId);
        }
        return toResponse(task);
    }

    /**
     * 分页查询任务列表
     *
     * @param status   状态筛选（可选）
     * @param metricId 指标ID筛选（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 任务列表
     */
    public List<TaskResponse> list(String status, Long metricId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Task> tasks = taskMapper.selectByPage(offset, pageSize, status, metricId);
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 统计符合条件的任务数量
     *
     * @param status   状态筛选（可选）
     * @param metricId 指标ID筛选（可选）
     * @return 数量
     */
    public long count(String status, Long metricId) {
        return taskMapper.countByCondition(status, metricId);
    }

    /**
     * 更新任务状态
     *
     * <p>由TaskConsumerService在任务开始执行时调用。
     *
     * @param taskId     任务ID
     * @param status     新状态
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param errorMessage 错误信息
     */
    @Transactional
    public void updateTaskStatus(String taskId, String status, LocalDateTime startTime,
                                 LocalDateTime endTime, String errorMessage) {
        Task task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("Task not found: taskId={}", taskId);
            return;
        }

        task.setStatus(status);
        if (startTime != null) {
            task.setStartTime(startTime);
        }
        if (endTime != null) {
            task.setEndTime(endTime);
        }
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }

        int updated = taskMapper.updateById(task);
        if (updated == 0) {
            log.warn("Task status not updated, task may not exist: taskId={}", taskId);
        }
        log.info("Task status updated: taskId={}, status={}", taskId, status);
    }

    /**
     * 更新任务执行结果
     *
     * <p>在任务执行完成（成功或失败）时调用，记录结果数据或错误信息。
     *
     * @param taskId       任务ID
     * @param status       最终状态（success/failed）
     * @param resultData   查询结果数据
     * @param errorMessage 错误信息
     */
    @Transactional
    public void updateTaskResult(String taskId, String status, Object resultData, String errorMessage) {
        Task task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("Task not found: taskId={}", taskId);
            return;
        }

        String resultDataJson = null;
        if (resultData != null) {
            try {
                resultDataJson = objectMapper.writeValueAsString(resultData);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize result data", e);
            }
        }

        task.setStatus(status);
        task.setResultData(resultDataJson);
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());

        int updated = taskMapper.updateResult(taskId, status, resultDataJson, errorMessage);
        if (updated == 0) {
            log.warn("Task result not updated, task may not exist: taskId={}", taskId);
        }
        log.info("Task result updated: taskId={}, status={}", taskId, status);
    }

    /**
     * 增加任务重试次数
     *
     * <p>判断是否可以继续重试，返回true表示可以重试，false表示已达最大重试次数。
     *
     * @param taskId 任务ID
     * @return 是否可以继续重试
     */
    @Transactional
    public boolean incrementRetryCount(String taskId) {
        Task task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            return false;
        }

        int newRetryCount = task.getRetryCount() + 1;
        task.setRetryCount(newRetryCount);
        task.setEndTime(LocalDateTime.now());

        int updated = taskMapper.updateResult(taskId, TaskStatus.PENDING.getValue(),
                task.getResultData(), task.getErrorMessage());
        if (updated == 0) {
            log.error("Failed to update task retry status, taskId={}", taskId);
            return false;
        }

        return newRetryCount < RabbitMQConfig.MAX_RETRY_COUNT;
    }

    /**
     * 发送任务消息到RabbitMQ
     */
    private void sendTaskMessage(Task task) {
        try {
            TaskMessage message = new TaskMessage();
            message.setTaskId(task.getTaskId());
            message.setMetricId(task.getMetricId());
            message.setAttemptCount(task.getRetryCount() + 1);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    RabbitMQConfig.TASK_ROUTING_KEY,
                    message
            );

            log.info("Task message sent to queue: taskId={}", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task message: taskId={}", task.getTaskId(), e);
            throw new BusinessException(ErrorCode.MESSAGE_QUEUE_ERROR, "任务入队失败");
        }
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(task.getTaskId());
        response.setMetricId(task.getMetricId());
        response.setStatus(task.getStatus());
        response.setStartTime(task.getStartTime());
        response.setEndTime(task.getEndTime());
        response.setRetryCount(task.getRetryCount());
        response.setCreatedAt(task.getCreatedAt());

        if (task.getResultData() != null) {
            try {
                response.setResultData(objectMapper.readValue(task.getResultData(), Object.class));
            } catch (JsonProcessingException e) {
                response.setResultData(task.getResultData());
            }
        }

        if (task.getErrorMessage() != null) {
            response.setErrorMessage(task.getErrorMessage());
        }

        return response;
    }

    /**
     * 任务消息体
     *
     * <p>用于RabbitMQ消息传递，包含任务执行所需信息。
     */
    @Data
    public static class TaskMessage {
        private String taskId;
        private Long metricId;
        private int attemptCount;
    }
}