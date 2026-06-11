-- 素材明细数据表
CREATE TABLE IF NOT EXISTS asset (
    asset_id VARCHAR(50) PRIMARY KEY COMMENT '素材ID',
    title VARCHAR(200) NOT NULL COMMENT '素材标题',
    uploader VARCHAR(50) NOT NULL COMMENT '上传人',
    uploaded_at DATETIME NOT NULL COMMENT '上传时间',
    file_size_bytes BIGINT NOT NULL COMMENT '文件大小（字节）',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态: approved/rejected/pending',
    tags VARCHAR(500) COMMENT '标签，逗号分隔',
    city VARCHAR(50) COMMENT '城市',
    platform VARCHAR(50) COMMENT '投放平台: 抖音/快手/小红书',
    duration_seconds INT DEFAULT 0 COMMENT '视频时长（秒）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_uploader (uploader),
    INDEX idx_city (city),
    INDEX idx_platform (platform),
    INDEX idx_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材明细数据表';

-- 指标配置表
CREATE TABLE IF NOT EXISTS metric_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    description VARCHAR(500) COMMENT '指标描述',
    source_table VARCHAR(50) NOT NULL DEFAULT 'asset' COMMENT '来源数据表',
    field VARCHAR(50) NOT NULL COMMENT '统计字段',
    aggregation VARCHAR(20) NOT NULL COMMENT '聚合方式: COUNT/SUM/AVG/MAX/MIN',
    group_by VARCHAR(50) COMMENT '分组维度字段',
    filter_condition VARCHAR(500) COMMENT '固定筛选条件，SQL片段',
    sort_rule VARCHAR(100) DEFAULT 'value DESC' COMMENT '排序规则',
    enabled TINYINT(1) DEFAULT 1 COMMENT '启用状态: 1启用 0停用',
    version INT DEFAULT 0 COMMENT '版本号，用于乐观锁',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_metric_name (metric_name),
    INDEX idx_enabled (enabled),
    INDEX idx_source_table (source_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标配置表';

-- 查询任务表
CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '任务唯一标识',
    metric_id BIGINT NOT NULL COMMENT '关联的指标配置ID',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态: pending/running/success/failed',
    start_time DATETIME COMMENT '开始执行时间',
    end_time DATETIME COMMENT '结束时间',
    result_data LONGTEXT COMMENT '执行结果（JSON格式）',
    error_message VARCHAR(1000) COMMENT '失败原因',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_metric_id (metric_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_task_metric FOREIGN KEY (metric_id) REFERENCES metric_config(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询任务表';