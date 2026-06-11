package com.example.metrics.controller;

import com.example.metrics.model.dto.request.CreateTaskRequest;
import com.example.metrics.model.dto.response.ApiResponse;
import com.example.metrics.model.dto.response.TaskResponse;
import com.example.metrics.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务管理接口
 *
 * <p>提供异步查询任务的创建和状态查询功能：
 * <ul>
 *   <li>创建查询任务（异步执行）</li>
 *   <li>查询任务状态和结果</li>
 *   <li>分页查询任务列表</li>
 * </ul>
 *
 * <p>任务创建后会进入RabbitMQ队列，由后台消费者异步执行。
 * 执行结果通过GET /tasks/{taskId}接口查询。
 *
 * @see com.example.metrics.service.TaskService
 * @see com.example.metrics.service.TaskConsumerService
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 创建查询任务
     *
     * <p>根据指定的指标配置创建异步查询任务，任务将进入消息队列等待执行。
     * 支持幂等性：若该指标存在pending/running状态的任务，直接返回原任务。
     *
     * @param request 包含metricId的请求体
     * @return 创建的任务信息，包含taskId和初始状态
     */
    @PostMapping
    public ApiResponse<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.create(request);
        return ApiResponse.success(response);
    }

    /**
     * 查询任务状态和结果
     *
     * @param taskId 任务ID（创建任务时返回的taskId）
     * @return 任务信息，包含状态、结果数据或错误信息
     */
    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getByTaskId(@PathVariable String taskId) {
        TaskResponse response = taskService.getByTaskId(taskId);
        return ApiResponse.success(response);
    }

    /**
     * 分页查询任务列表
     *
     * @param status   筛选任务状态（可选：pending/running/success/failed）
     * @param metricId 筛选指标ID（可选）
     * @param pageNum  页码（默认1）
     * @param pageSize 每页数量（默认20，最大100）
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long metricId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 分页参数边界校验：统一在 [1, 100] 范围内
        pageNum = Math.max(1, pageNum);
        pageSize = Math.min(100, Math.max(1, pageSize));

        List<TaskResponse> list = taskService.list(status, metricId, pageNum, pageSize);
        long total = taskService.count(status, metricId);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);

        return ApiResponse.success(data);
    }
}
