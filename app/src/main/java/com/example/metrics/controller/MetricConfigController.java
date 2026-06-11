package com.example.metrics.controller;


import com.example.metrics.model.dto.request.CreateMetricConfigRequest;
import com.example.metrics.model.dto.request.UpdateMetricConfigRequest;
import com.example.metrics.model.dto.request.UpdateStatusRequest;
import com.example.metrics.model.dto.response.ApiResponse;
import com.example.metrics.model.dto.response.MetricConfigResponse;
import com.example.metrics.service.MetricConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标配置管理接口
 *
 * <p>提供指标配置的CRUD操作，包括：
 * <ul>
 *   <li>新增指标配置</li>
 *   <li>更新指标配置</li>
 *   <li>查询指标配置详情</li>
 *   <li>分页查询指标配置列表</li>
 *   <li>启用/停用指标配置</li>
 *   <li>校验指标配置合法性</li>
 *   <li>删除指标配置</li>
 * </ul>
 *
 * @see com.example.metrics.service.MetricConfigService
 */
@RestController
@RequestMapping("/api/v1/metrics/configs")
public class MetricConfigController {

    private final MetricConfigService metricConfigService;

    public MetricConfigController(MetricConfigService metricConfigService) {
        this.metricConfigService = metricConfigService;
    }

    /**
     * 新增指标配置
     *
     * @param request 指标配置信息
     * @return 新建的指标配置
     */
    @PostMapping
    public ApiResponse<MetricConfigResponse> create(
            @Valid @RequestBody CreateMetricConfigRequest request) {
        MetricConfigResponse response = metricConfigService.create(request);
        return ApiResponse.success(response);
    }

    /**
     * 更新指标配置
     *
     * @param id 指标配置ID
     * @param request 更新后的指标配置信息
     * @return 更新后的指标配置
     */
    @PutMapping("/{id}")
    public ApiResponse<MetricConfigResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMetricConfigRequest request) {
        MetricConfigResponse response = metricConfigService.update(id, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取指标配置详情
     *
     * @param id 指标配置ID
     * @return 指标配置详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MetricConfigResponse> getById(@PathVariable Long id) {
        MetricConfigResponse response = metricConfigService.getById(id);
        return ApiResponse.success(response);
    }

    /**
     * 分页查询指标配置列表
     *
     * @param enabled  筛选启用状态（可选）
     * @param keyword  搜索关键词（可选）
     * @param pageNum  页码（默认1）
     * @param pageSize 每页数量（默认20，最大100）
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 分页参数边界校验：统一在 [1, 100] 范围内
        pageNum = Math.max(1, pageNum);
        pageSize = Math.min(100, Math.max(1, pageSize));

        List<MetricConfigResponse> list = metricConfigService.list(enabled, keyword, pageNum, pageSize);
        long total = metricConfigService.count(enabled, keyword);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);

        return ApiResponse.success(data);
    }

    /**
     * 启用/停用指标配置
     *
     * @param id      指标配置ID
     * @param request 请求体，包含enabled字段
     */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<Void> updateEnabled(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        metricConfigService.updateEnabled(id, request.getEnabled());
        return ApiResponse.success(null);
    }

    /**
     * 校验指标配置合法性
     *
     * <p>校验内容包括：表名、字段名、聚合方式、筛选条件等
     *
     * @param id 指标配置ID
     */
    @PostMapping("/validate/{id}")
    public ApiResponse<Void> validate(@PathVariable Long id) {
        metricConfigService.validateConfig(id);
        return ApiResponse.success(null);
    }

    /**
     * 删除指标配置
     *
     * @param id 指标配置ID
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        metricConfigService.delete(id);
        return ApiResponse.success(null);
    }
}
