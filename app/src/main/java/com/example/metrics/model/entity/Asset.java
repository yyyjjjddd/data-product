package com.example.metrics.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 素材实体
 *
 * <p>对应数据库中的asset表，存储视频素材的明细数据：
 * <ul>
 *   <li>asset_id: 素材唯一标识</li>
 *   <li>title: 素材标题</li>
 *   <li>uploader: 上传人</li>
 *   <li>uploaded_at: 上传时间</li>
 *   <li>file_size_bytes: 文件大小（字节）</li>
 *   <li>status: 审核状态（approved/rejected/pending）</li>
 *   <li>tags: 标签，多个用逗号分隔</li>
 *   <li>city: 城市</li>
 *   <li>platform: 投放平台（抖音/快手/小红书）</li>
 *   <li>duration_seconds: 视频时长（秒）</li>
 * </ul>
 *
 * <p>作为指标统计的原始数据来源，支持按城市、平台、审核状态等维度统计。
 */
@Data
@NoArgsConstructor
public class Asset {

    private String assetId;
    private String title;
    private String uploader;
    private LocalDateTime uploadedAt;
    private Long fileSizeBytes;
    private String status;
    private String tags;
    private String city;
    private String platform;
    private Integer durationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
