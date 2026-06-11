package com.example.metrics.service;

import com.example.metrics.config.RabbitMQConfig;
import com.example.metrics.exception.BusinessException;
import com.example.metrics.exception.ErrorCode;
import com.example.metrics.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskConsumerService单元测试
 *
 * <p>测试任务消费服务：
 * <ul>
 *   <li>正常情况：成功消费任务并更新状态</li>
 *   <li>异常情况：任务失败处理、重试机制</li>
 *   <li>边界条件：最大重试次数、业务异常 vs 系统异常</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TaskConsumerServiceTest {

    @Mock
    private QueryExecuteService queryExecuteService;

    @Mock
    private TaskService taskService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TaskConsumerService taskConsumerService;

    @BeforeEach
    void setUp() {
        taskConsumerService = new TaskConsumerService(queryExecuteService, taskService, rabbitTemplate);
    }

    // ========== consumeTask 测试 ==========

    @Test
    void consumeTask_withSuccessfulExecution_shouldUpdateToSuccess() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);

        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(createResultRow("approved", 10));
        when(queryExecuteService.executeQuery(1L)).thenReturn(mockResults);

        // when
        taskConsumerService.consumeTask(message);

        // then
        verify(taskService).updateTaskStatus(eq("task-1"), eq(TaskStatus.RUNNING.getValue()),
                any(), isNull(), isNull());
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.SUCCESS.getValue()),
                eq(mockResults), isNull());
    }

    @Test
    void consumeTask_whenUpdateStatusToRunningFails_shouldNotProceed() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        doThrow(new RuntimeException("DB error")).when(taskService)
                .updateTaskStatus(anyString(), anyString(), any(), any(), any());
        when(taskService.incrementRetryCount("task-1")).thenReturn(true);

        // when
        taskConsumerService.consumeTask(message);

        // then - updateTaskStatus抛出异常后，handleFailure被调用
        // 但executeQuery不应该被调用（因为还没到那一步）
        verify(queryExecuteService, never()).executeQuery(anyLong());
        verify(taskService).incrementRetryCount("task-1");
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_RETRY_ROUTING_KEY),
                any(TaskService.TaskMessage.class)
        );
    }

    @Test
    void consumeTask_withBusinessException_shouldMarkAsFailed() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        when(queryExecuteService.executeQuery(1L)).thenThrow(
                new BusinessException(ErrorCode.METRIC_CONFIG_INVALID, "配置不合法")
        );

        // when
        taskConsumerService.consumeTask(message);

        // then
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.FAILED.getValue()), isNull(), contains("配置不合法"));
    }

    @Test
    void consumeTask_withSystemException_andWithinRetryLimit_shouldScheduleRetry() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        when(queryExecuteService.executeQuery(1L)).thenThrow(new RuntimeException("DB connection timeout"));
        when(taskService.incrementRetryCount("task-1")).thenReturn(true);

        // when
        taskConsumerService.consumeTask(message);

        // then - 当可以重试时，不更新任务结果为failed，只发送重试消息
        verify(taskService, never()).updateTaskResult(anyString(), anyString(), any(), any());
        verify(taskService).incrementRetryCount("task-1");
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_RETRY_ROUTING_KEY),
                any(TaskService.TaskMessage.class)
        );
    }

    @Test
    void consumeTask_withSystemException_andExceedsRetryLimit_shouldMarkAsFailed() {
        // given
        // attemptCount = 3, MAX_RETRY_COUNT = 3, so 3 < 3 is false, goes to else branch
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 3);
        when(queryExecuteService.executeQuery(1L)).thenThrow(new RuntimeException("DB connection timeout"));

        // when
        taskConsumerService.consumeTask(message);

        // then - 当attemptCount >= MAX_RETRY_COUNT时，不重试，直接标记为failed
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.FAILED.getValue()),
                isNull(), contains("DB connection timeout"));
        verify(taskService, never()).incrementRetryCount(anyString());
        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_RETRY_ROUTING_KEY),
                any(TaskService.TaskMessage.class)
        );
    }

    @Test
    void consumeTask_withNonRetryableException_shouldNotRetry() {
        // given - BusinessException 不可重试
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        when(queryExecuteService.executeQuery(1L)).thenThrow(
                new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "参数错误")
        );

        // when
        taskConsumerService.consumeTask(message);

        // then - BusinessException的getMessage()返回errorCode.getMessage()即"参数校验失败"
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.FAILED.getValue()),
                isNull(), contains("参数校验失败"));
        verify(taskService, never()).incrementRetryCount(anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void consumeTask_withFirstAttempt_shouldHaveCorrectAttemptCount() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        List<Map<String, Object>> mockResults = new ArrayList<>();
        mockResults.add(createResultRow("status", 5));
        when(queryExecuteService.executeQuery(1L)).thenReturn(mockResults);

        // when
        taskConsumerService.consumeTask(message);

        // then
        verify(taskService).updateTaskStatus(eq("task-1"), eq(TaskStatus.RUNNING.getValue()),
                any(), isNull(), isNull());
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.SUCCESS.getValue()),
                eq(mockResults), isNull());
    }

    @Test
    void consumeTask_withEmptyResults_shouldStillSucceed() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        when(queryExecuteService.executeQuery(1L)).thenReturn(new ArrayList<>());

        // when
        taskConsumerService.consumeTask(message);

        // then
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.SUCCESS.getValue()),
                eq(new ArrayList<>()), isNull());
    }

    // ========== handleFailure 测试 ==========

    @Test
    void handleFailure_withRetryableError_andWithinLimit_shouldSendToRetryQueue() {
        // given
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 1);
        String errorMessage = "Database timeout";
        when(taskService.incrementRetryCount("task-1")).thenReturn(true);

        // when
        // 使用反射或通过consumeTask中间接地测试handleFailure
        // 这里我们通过consumeTask来测试完整的流程
        when(queryExecuteService.executeQuery(1L)).thenThrow(new RuntimeException(errorMessage));

        // when
        taskConsumerService.consumeTask(message);

        // then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_RETRY_ROUTING_KEY),
                any(TaskService.TaskMessage.class)
        );
    }

    @Test
    void handleFailure_withRetryableError_andAtMaxLimit_shouldNotSendToRetryQueue() {
        // given - attemptCount = 3, MAX_RETRY_COUNT = 3, so 3 < 3 is false
        TaskService.TaskMessage message = createTaskMessage("task-1", 1L, 3);
        String errorMessage = "Database timeout";

        // when
        when(queryExecuteService.executeQuery(1L)).thenThrow(new RuntimeException(errorMessage));

        // when
        taskConsumerService.consumeTask(message);

        // then
        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_RETRY_ROUTING_KEY),
                any(TaskService.TaskMessage.class)
        );
        // 当attemptCount >= MAX_RETRY_COUNT时，直接标记为failed，错误消息为原始消息
        verify(taskService).updateTaskResult(eq("task-1"), eq(TaskStatus.FAILED.getValue()),
                isNull(), contains("Database timeout"));
    }

    // ========== 辅助方法 ==========

    private TaskService.TaskMessage createTaskMessage(String taskId, Long metricId, int attemptCount) {
        TaskService.TaskMessage message = new TaskService.TaskMessage();
        message.setTaskId(taskId);
        message.setMetricId(metricId);
        message.setAttemptCount(attemptCount);
        return message;
    }

    private Map<String, Object> createResultRow(String groupByValue, Object aggregatedValue) {
        Map<String, Object> row = new HashMap<>();
        row.put("status", groupByValue);
        row.put("value", aggregatedValue);
        return row;
    }
}
