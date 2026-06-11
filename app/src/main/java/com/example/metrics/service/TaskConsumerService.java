package com.example.metrics.service;

/**
 * 任务消费服务
 *
 * <p>负责从RabbitMQ队列中消费任务并执行：
 * <ul>
 *   <li>监听任务队列，接收待执行任务</li>
 *   <li>更新任务状态为running</li>
 *   <li>调用QueryExecuteService执行查询</li>
 *   <li>更新任务结果或处理失败</li>
 *   <li>实现失败重试机制</li>
 * </ul>
 *
 * <p>使用Spring AMQP的@RabbitListener注解实现消息监听，
 * 采用手动Ack模式确保消息处理可靠性。
 *
 * <p>重试机制：
 * <ul>
 *   <li>可重试错误（数据库超时等）：通过重试队列延迟重试</li>
 *   <li>不可重试错误（配置错误等）：直接标记为failed</li>
 *   <li>最大重试次数：3次（可配置）</li>
 * </ul>
 *
 * @see com.example.metrics.config.RabbitMQConfig
 * @see com.example.metrics.service.QueryExecuteService
 */

import com.example.metrics.config.RabbitMQConfig;
import com.example.metrics.exception.BusinessException;
import com.example.metrics.model.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务消费服务
 *
 * <p>负责从RabbitMQ队列中消费任务并执行：
 * <ul>
 *   <li>监听任务队列，接收待执行任务</li>
 *   <li>更新任务状态为running</li>
 *   <li>调用QueryExecuteService执行查询</li>
 *   <li>更新任务结果或处理失败</li>
 *   <li>实现失败重试机制</li>
 * </ul>
 *
 * <p>使用Spring AMQP的@RabbitListener注解实现消息监听，
 * 采用手动Ack模式确保消息处理可靠性。
 *
 * <p>重试机制：
 * <ul>
 *   <li>可重试错误（数据库超时等）：通过重试队列延迟重试</li>
 *   <li>不可重试错误（配置错误等）：直接标记为failed</li>
 *   <li>最大重试次数：3次（可配置）</li>
 * </ul>
 *
 * @see com.example.metrics.config.RabbitMQConfig
 * @see com.example.metrics.service.QueryExecuteService
 */
@Service
public class TaskConsumerService {

    private static final Logger log = LoggerFactory.getLogger(TaskConsumerService.class);

    private final QueryExecuteService queryExecuteService;
    private final TaskService taskService;
    private final RabbitTemplate rabbitTemplate;

    public TaskConsumerService(QueryExecuteService queryExecuteService, TaskService taskService,
                               RabbitTemplate rabbitTemplate) {
        this.queryExecuteService = queryExecuteService;
        this.taskService = taskService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 消费任务消息
     *
     * <p>使用@RabbitListener监听任务队列，接收到消息后：
     * <ol>
     *   <li>更新任务状态为running</li>
     *   <li>执行指标查询</li>
     *   <li>更新任务结果为success</li>
     *   <li>如有异常，根据类型处理失败或重试</li>
     * </ol>
     *
     * @param message 任务消息体
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void consumeTask(TaskService.TaskMessage message) {
        String taskId = message.getTaskId();
        log.info("Consuming task: taskId={}, attempt={}", taskId, message.getAttemptCount());

        try {
            try {
                taskService.updateTaskStatus(taskId, TaskStatus.RUNNING.getValue(),
                        LocalDateTime.now(), null, null);
            } catch (Exception e) {
                log.error("Failed to update task status to RUNNING, taskId={}", taskId, e);
                throw e;
            }

            List<Map<String, Object>> result = queryExecuteService.executeQuery(message.getMetricId());

            taskService.updateTaskResult(taskId, TaskStatus.SUCCESS.getValue(), result, null);

            log.info("Task executed successfully: taskId={}", taskId);

        } catch (BusinessException e) {
            log.error("Task execution failed (business error): taskId={}, error={}",
                    taskId, e.getMessage());
            handleFailure(message, e.getMessage(), false);

        } catch (Exception e) {
            log.error("Task execution failed (system error): taskId={}", taskId, e);
            handleFailure(message, e.getMessage(), true);
        }
    }

    /**
     * 处理任务失败
     *
     * <p>根据错误类型和重试次数决定：
     * <ul>
     *   <li>可重试且未达最大次数：发送到重试队列</li>
     *   <li>不可重试或已达最大次数：标记为failed</li>
     * </ul>
     *
     * @param message     任务消息
     * @param errorMessage 错误信息
     * @param retryable   是否可重试
     */
    private void handleFailure(TaskService.TaskMessage message, String errorMessage, boolean retryable) {
        String taskId = message.getTaskId();
        int attemptCount = message.getAttemptCount();

        if (retryable && attemptCount < RabbitMQConfig.MAX_RETRY_COUNT) {
            log.info("Scheduling retry for task: taskId={}, attempt={}", taskId, attemptCount + 1);

            boolean canRetry = taskService.incrementRetryCount(taskId);

            if (canRetry) {
                message.setAttemptCount(attemptCount + 1);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.TASK_EXCHANGE,
                        RabbitMQConfig.TASK_RETRY_ROUTING_KEY,
                        message
                );
            } else {
                taskService.updateTaskResult(taskId, TaskStatus.FAILED.getValue(),
                        null, "超过最大重试次数: " + errorMessage);
            }
        } else {
            taskService.updateTaskResult(taskId, TaskStatus.FAILED.getValue(),
                    null, errorMessage);
        }
    }
}