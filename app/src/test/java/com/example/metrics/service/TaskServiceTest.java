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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskService单元测试
 *
 * <p>测试任务业务逻辑：
 * <ul>
 *   <li>正常情况：创建任务、查询状态、列表查询</li>
 *   <li>异常情况：配置不存在、配置停用、任务不存在</li>
 *   <li>边界条件：幂等性、重试次数、超时</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private MetricConfigService metricConfigService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        taskService = new TaskService(taskMapper, metricConfigService, rabbitTemplate, objectMapper);
    }

    // ========== create 测试 ==========

    @Test
    void create_withValidRequest_shouldCreateTask() {
        // given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setMetricId(1L);

        MetricConfig config = createEnabledMetricConfig(1L);
        when(metricConfigService.getEntityById(1L)).thenReturn(config);
        when(taskMapper.selectPendingOrRunningByMetricId(1L)).thenReturn(null);
        when(taskMapper.insert(any(Task.class))).thenReturn(1L);

        // when
        TaskResponse response = taskService.create(request);

        // then
        assertNotNull(response);
        assertNotNull(response.getTaskId());
        assertEquals(1L, response.getMetricId());
        assertEquals(TaskStatus.PENDING.getValue(), response.getStatus());
        assertEquals(0, response.getRetryCount());
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.TASK_EXCHANGE), eq(RabbitMQConfig.TASK_ROUTING_KEY), any(TaskService.TaskMessage.class)
        );
    }

    @Test
    void create_withDisabledConfig_shouldThrowException() {
        // given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setMetricId(1L);

        MetricConfig config = createEnabledMetricConfig(1L);
        config.setEnabled(false);
        when(metricConfigService.getEntityById(1L)).thenReturn(config);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                taskService.create(request)
        );
        assertEquals(ErrorCode.METRIC_CONFIG_INVALID, exception.getErrorCode());
        assertTrue(exception.getDetails().contains("已停用"));
    }

    @Test
    void create_withExistingPendingTask_shouldReturnExistingTask() {
        // given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setMetricId(1L);

        MetricConfig config = createEnabledMetricConfig(1L);
        Task existingTask = createExistingTask("existing-task-id", 1L, TaskStatus.PENDING);

        when(metricConfigService.getEntityById(1L)).thenReturn(config);
        when(taskMapper.selectPendingOrRunningByMetricId(1L)).thenReturn(existingTask);

        // when
        TaskResponse response = taskService.create(request);

        // then
        assertNotNull(response);
        assertEquals("existing-task-id", response.getTaskId());
        verify(taskMapper, never()).insert(any(Task.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void create_withExistingRunningTask_shouldReturnExistingTask() {
        // given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setMetricId(1L);

        MetricConfig config = createEnabledMetricConfig(1L);
        Task existingTask = createExistingTask("existing-task-id", 1L, TaskStatus.RUNNING);

        when(metricConfigService.getEntityById(1L)).thenReturn(config);
        when(taskMapper.selectPendingOrRunningByMetricId(1L)).thenReturn(existingTask);

        // when
        TaskResponse response = taskService.create(request);

        // then
        assertEquals("existing-task-id", response.getTaskId());
        assertEquals(TaskStatus.RUNNING.getValue(), response.getStatus());
    }

    @Test
    void create_whenDatabaseInsertFails_shouldThrowException() {
        // given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setMetricId(1L);

        MetricConfig config = createEnabledMetricConfig(1L);
        when(metricConfigService.getEntityById(1L)).thenReturn(config);
        when(taskMapper.selectPendingOrRunningByMetricId(1L)).thenReturn(null);
        when(taskMapper.insert(any(Task.class))).thenReturn(0L);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                taskService.create(request)
        );
        assertEquals(ErrorCode.DATABASE_ERROR, exception.getErrorCode());
    }

    @Test
    void create_whenMessageQueueFails_shouldThrowException() {
        // given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setMetricId(1L);

        MetricConfig config = createEnabledMetricConfig(1L);
        when(metricConfigService.getEntityById(1L)).thenReturn(config);
        when(taskMapper.selectPendingOrRunningByMetricId(1L)).thenReturn(null);
        when(taskMapper.insert(any(Task.class))).thenReturn(1L);
        doThrow(new RuntimeException("MQ连接失败")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                taskService.create(request)
        );
        assertEquals(ErrorCode.MESSAGE_QUEUE_ERROR, exception.getErrorCode());
    }

    // ========== getByTaskId 测试 ==========

    @Test
    void getByTaskId_withExistingTaskId_shouldReturnTask() {
        // given
        String taskId = "test-task-id";
        Task task = createExistingTask(taskId, 1L, TaskStatus.SUCCESS);
        task.setResultData("[{\"status\":\"approved\",\"value\":10}]");
        when(taskMapper.selectByTaskId(taskId)).thenReturn(task);

        // when
        TaskResponse response = taskService.getByTaskId(taskId);

        // then
        assertNotNull(response);
        assertEquals(taskId, response.getTaskId());
        assertEquals(TaskStatus.SUCCESS.getValue(), response.getStatus());
        assertNotNull(response.getResultData());
    }

    @Test
    void getByTaskId_withNonExistentTaskId_shouldThrowException() {
        // given
        String taskId = "non-existent-task-id";
        when(taskMapper.selectByTaskId(taskId)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                taskService.getByTaskId(taskId)
        );
        assertEquals(ErrorCode.TASK_NOT_FOUND, exception.getErrorCode());
    }

    // ========== list 测试 ==========

    @Test
    void list_withPagination_shouldReturnTasks() {
        // given
        int pageNum = 1;
        int pageSize = 10;
        List<Task> tasks = Arrays.asList(
                createExistingTask("task-1", 1L, TaskStatus.SUCCESS),
                createExistingTask("task-2", 1L, TaskStatus.FAILED)
        );
        when(taskMapper.selectByPage(0, 10, null, null)).thenReturn(tasks);

        // when
        List<TaskResponse> responses = taskService.list(null, null, pageNum, pageSize);

        // then
        assertEquals(2, responses.size());
        verify(taskMapper).selectByPage(0, 10, null, null);
    }

    @Test
    void list_withStatusFilter_shouldFilterByStatus() {
        // given
        String status = TaskStatus.SUCCESS.getValue();
        List<Task> tasks = Arrays.asList(createExistingTask("task-1", 1L, TaskStatus.SUCCESS));
        when(taskMapper.selectByPage(0, 10, status, null)).thenReturn(tasks);

        // when
        List<TaskResponse> responses = taskService.list(status, null, 1, 10);

        // then
        assertEquals(1, responses.size());
        assertEquals(TaskStatus.SUCCESS.getValue(), responses.get(0).getStatus());
    }

    @Test
    void list_withMetricIdFilter_shouldFilterByMetricId() {
        // given
        Long metricId = 1L;
        List<Task> tasks = Arrays.asList(createExistingTask("task-1", metricId, TaskStatus.PENDING));
        when(taskMapper.selectByPage(0, 10, null, metricId)).thenReturn(tasks);

        // when
        List<TaskResponse> responses = taskService.list(null, metricId, 1, 10);

        // then
        assertEquals(1, responses.size());
        assertEquals(metricId, responses.get(0).getMetricId());
    }

    @Test
    void list_withLargePageNum_shouldCalculateCorrectOffset() {
        // given
        int pageNum = 10;
        int pageSize = 20;
        when(taskMapper.selectByPage(anyInt(), anyInt(), any(), any())).thenReturn(List.of());

        // when
        taskService.list(null, null, pageNum, pageSize);

        // then
        verify(taskMapper).selectByPage(180, 20, null, null); // (10-1)*20 = 180
    }

    // ========== count 测试 ==========

    @Test
    void count_shouldReturnTotalCount() {
        // given
        when(taskMapper.countByCondition(null, null)).thenReturn(50L);

        // when
        long count = taskService.count(null, null);

        // then
        assertEquals(50L, count);
    }

    @Test
    void count_withFilters_shouldCountFilteredResults() {
        // given
        String status = TaskStatus.FAILED.getValue();
        when(taskMapper.countByCondition(status, null)).thenReturn(10L);

        // when
        long count = taskService.count(status, null);

        // then
        assertEquals(10L, count);
    }

    // ========== updateTaskStatus 测试 ==========

    @Test
    void updateTaskStatus_withValidData_shouldUpdateStatus() {
        // given
        String taskId = "test-task-id";
        Task task = createExistingTask(taskId, 1L, TaskStatus.PENDING);
        when(taskMapper.selectByTaskId(taskId)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        // when
        LocalDateTime startTime = LocalDateTime.now();
        taskService.updateTaskStatus(taskId, TaskStatus.RUNNING.getValue(), startTime, null, null);

        // then
        verify(taskMapper).updateById(any(Task.class));
    }

    @Test
    void updateTaskStatus_withNonExistentTask_shouldNotThrow() {
        // given
        String taskId = "non-existent-task-id";
        when(taskMapper.selectByTaskId(taskId)).thenReturn(null);

        // when & then - 不抛异常
        assertDoesNotThrow(() ->
                taskService.updateTaskStatus(taskId, TaskStatus.RUNNING.getValue(), LocalDateTime.now(), null, null)
        );
    }

    @Test
    void updateTaskStatus_withUpdateFailure_shouldNotThrow() {
        // given
        String taskId = "test-task-id";
        Task task = createExistingTask(taskId, 1L, TaskStatus.PENDING);
        when(taskMapper.selectByTaskId(taskId)).thenReturn(task);
        when(taskMapper.updateById(any(Task.class))).thenReturn(0);

        // when & then - 不抛异常，只记录warn日志
        assertDoesNotThrow(() ->
                taskService.updateTaskStatus(taskId, TaskStatus.RUNNING.getValue(), LocalDateTime.now(), null, null)
        );
    }

    @Test
    void updateTaskResult_withFailure_shouldUpdateErrorMessage() {
        // given
        String taskId = "test-task-id";
        Task task = createExistingTask(taskId, 1L, TaskStatus.RUNNING);
        String errorMessage = "SQL执行超时";
        when(taskMapper.selectByTaskId(taskId)).thenReturn(task);
        when(taskMapper.updateResult(eq(taskId), eq(TaskStatus.FAILED.getValue()), isNull(), eq(errorMessage)))
                .thenReturn(1);

        // when
        taskService.updateTaskResult(taskId, TaskStatus.FAILED.getValue(), null, errorMessage);

        // then
        verify(taskMapper).updateResult(eq(taskId), eq(TaskStatus.FAILED.getValue()), isNull(), eq(errorMessage));
    }

    // ========== incrementRetryCount 测试 ==========

    @Test
    void incrementRetryCount_withIncompleteRetry_shouldReturnTrue() {
        // given
        String taskId = "test-task-id";
        Task task = createExistingTask(taskId, 1L, TaskStatus.FAILED);
        task.setRetryCount(1); // 已经重试1次
        when(taskMapper.selectByTaskId(taskId)).thenReturn(task);
        when(taskMapper.updateResult(anyString(), anyString(), any(), any())).thenReturn(1);

        // when
        boolean canRetry = taskService.incrementRetryCount(taskId);

        // then
        assertTrue(canRetry); // 3次最大，还可以重试2次
    }

    @Test
    void incrementRetryCount_withMaxRetry_shouldReturnFalse() {
        // given
        String taskId = "test-task-id";
        Task task = createExistingTask(taskId, 1L, TaskStatus.FAILED);
        task.setRetryCount(3); // 已达最大重试次数
        when(taskMapper.selectByTaskId(taskId)).thenReturn(task);
        when(taskMapper.updateResult(anyString(), anyString(), any(), any())).thenReturn(1);

        // when
        boolean canRetry = taskService.incrementRetryCount(taskId);

        // then
        assertFalse(canRetry);
    }

    @Test
    void incrementRetryCount_withNonExistentTask_shouldReturnFalse() {
        // given
        String taskId = "non-existent-task-id";
        when(taskMapper.selectByTaskId(taskId)).thenReturn(null);

        // when
        boolean canRetry = taskService.incrementRetryCount(taskId);

        // then
        assertFalse(canRetry);
    }

    // ========== 辅助方法 ==========

    private MetricConfig createEnabledMetricConfig(Long id) {
        MetricConfig config = new MetricConfig();
        config.setId(id);
        config.setMetricName("test_metric");
        config.setEnabled(true);
        config.setSourceTable("asset");
        config.setField("asset_id");
        config.setAggregation("COUNT");
        return config;
    }

    private Task createExistingTask(String taskId, Long metricId, TaskStatus status) {
        Task task = new Task();
        task.setId(1L);
        task.setTaskId(taskId);
        task.setMetricId(metricId);
        task.setStatus(status.getValue());
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}

