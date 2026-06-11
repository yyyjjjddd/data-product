package com.example.metrics.controller;

import com.example.metrics.mapper.AssetMapper;
import com.example.metrics.model.dto.response.ApiResponse;
import com.example.metrics.model.entity.Asset;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 素材数据接口
 *
 * <p>提供素材数据的查询功能：
 * <ul>
 *   <li>分页查询素材列表</li>
 *   <li>查询素材详情</li>
 *   <li>查询素材统计摘要</li>
 * </ul>
 *
 * <p>素材数据是指标统计的原始数据来源。
 *
 * @see com.example.metrics.model.entity.Asset
 */
@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetMapper assetMapper;

    public AssetController(AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    /**
     * 分页查询素材列表
     *
     * @param pageNum  页码（默认1）
     * @param pageSize 每页数量（默认20，最大100）
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 分页参数边界校验：统一在 [1, 100] 范围内
        pageNum = Math.max(1, pageNum);
        pageSize = Math.min(100, Math.max(1, pageSize));

        int offset = (pageNum - 1) * pageSize;
        List<Asset> list = assetMapper.selectByPagination(offset, pageSize);
        long total = assetMapper.count();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);

        return ApiResponse.success(data);
    }

    /**
     * 查询素材详情
     *
     * @param assetId 素材ID
     * @return 素材详情
     */
    @GetMapping("/{assetId}")
    public ApiResponse<Asset> getById(@PathVariable String assetId) {
        Asset asset = assetMapper.selectById(assetId);
        return ApiResponse.success(asset);
    }

    /**
     * 查询素材统计摘要
     *
     * @return 统计摘要信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", assetMapper.count());
        return ApiResponse.success(stats);
    }
}

