# 指标配置后台服务 - API 文档

## 一、接口列表

### 1.1 指标配置接口 (MetricConfigController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /metrics/configs | 新增指标配置 |
| PUT | /metrics/configs/{id} | 更新指标配置 |
| GET | /metrics/configs/{id} | 获取指标配置详情 |
| GET | /metrics/configs | 分页查询指标配置列表 |
| PATCH | /metrics/configs/{id}/enabled | 启用/停用指标配置 |
| POST | /metrics/configs/validate/{id} | 校验指标配置合法性 |
| DELETE | /metrics/configs/{id} | 删除指标配置 |

### 1.2 任务接口 (TaskController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /tasks | 创建查询任务 |
| GET | /tasks/{taskId} | 查询任务状态和结果 |
| GET | /tasks | 分页查询任务列表 |

### 1.3 素材接口 (AssetController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /assets | 分页查询素材列表 |
| GET | /assets/{assetId} | 查询素材详情 |
| GET | /assets/stats | 查询素材统计摘要 |

---

## 二、接口详情

### 2.1 指标配置接口

#### 2.1.1 新增指标配置

**接口路径:** `POST /metrics/configs`

**接口含义:** 创建一个新的指标配置，用于定义统计指标的查询规则。

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| metricName | string | 是 | 指标名称，长度1-100 | 各城市素材数量 |
| description | string | 否 | 指标描述，最大500字符 | 统计各城市的素材数量 |
| sourceTable | string | 是 | 来源数据表 | asset |
| field | string | 是 | 统计字段，需在白名单内 | asset_id |
| aggregation | string | 是 | 聚合方式：COUNT/SUM/AVG/MAX/MIN | COUNT |
| groupBy | string | 否 | 分组维度字段 | city |
| filterCondition | string | 否 | 固定筛选条件，SQL片段 | status='approved' |
| sortRule | string | 否 | 排序规则 | value DESC |
| enabled | boolean | 否 | 是否启用，默认true | true |

**field 白名单字段:**

| 字段名 | 可用聚合 | 说明 |
|--------|----------|------|
| asset_id | COUNT | 素材ID |
| file_size_bytes | COUNT/SUM/AVG/MAX/MIN | 文件大小（字节） |
| duration_seconds | COUNT/SUM/AVG/MAX/MIN | 视频时长（秒） |
| uploader | COUNT | 上传人 |
| status | COUNT | 审核状态 |
| city | COUNT | 城市 |
| platform | COUNT | 投放平台 |

**请求示例:**
```json
{
  "metricName": "各城市素材数量",
  "description": "统计各城市的素材数量",
  "sourceTable": "asset",
  "field": "asset_id",
  "aggregation": "COUNT",
  "groupBy": "city",
  "filterCondition": null,
  "sortRule": "value DESC"
}
```

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | long | 指标配置ID |
| metricName | string | 指标名称 |
| enabled | boolean | 是否启用 |
| createdAt | string | 创建时间 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "metricName": "各城市素材数量",
    "enabled": true,
    "createdAt": "2026-06-04T10:00:00"
  }
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40001 | 参数校验失败（必填字段缺失或格式错误） |
| 40003 | 指标名称重复 |
| 40004 | 配置不合法（字段/表不存在） |
| 40005 | 聚合方式与字段类型不匹配 |

---

#### 2.1.2 更新指标配置

**接口路径:** `PUT /metrics/configs/{id}`

**接口含义:** 更新指定指标配置的内容。传入的非空字段会被更新，空字段保持不变。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 指标配置ID |

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| metricName | string | 否 | 指标名称，长度1-100 |
| description | string | 否 | 指标描述，最大500字符 |
| sourceTable | string | 否 | 来源数据表 |
| field | string | 否 | 统计字段 |
| aggregation | string | 否 | 聚合方式 |
| groupBy | string | 否 | 分组维度字段 |
| filterCondition | string | 否 | 固定筛选条件 |
| sortRule | string | 否 | 排序规则 |
| enabled | boolean | 否 | 是否启用 |

**请求示例:**
```json
{
  "metricName": "各城市素材数量(已修改)",
  "description": "统计各城市的素材数量分布",
  "field": "asset_id",
  "aggregation": "COUNT",
  "groupBy": "city",
  "sortRule": "value ASC"
}
```

**响应参数:** 返回更新后的指标配置信息，结构同2.1.3详情接口。

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40001 | 参数校验失败 |
| 40002 | 指标配置不存在 |
| 40003 | 指标名称重复（与其他配置重名） |
| 40004 | 配置不合法 |
| 40005 | 聚合方式与字段类型不匹配 |

---

#### 2.1.3 获取指标配置详情

**接口路径:** `GET /metrics/configs/{id}`

**接口含义:** 根据ID获取指标配置的完整信息。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 指标配置ID |

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | long | 指标配置ID |
| metricName | string | 指标名称 |
| description | string | 指标描述 |
| sourceTable | string | 来源数据表 |
| field | string | 统计字段 |
| aggregation | string | 聚合方式 |
| groupBy | string | 分组维度 |
| filterCondition | string | 固定筛选条件 |
| sortRule | string | 排序规则 |
| enabled | boolean | 是否启用 |
| createdAt | string | 创建时间 |
| updatedAt | string | 更新时间 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "metricName": "各城市素材数量",
    "description": "统计各城市的素材数量",
    "sourceTable": "asset",
    "field": "asset_id",
    "aggregation": "COUNT",
    "groupBy": "city",
    "filterCondition": null,
    "sortRule": "value DESC",
    "enabled": true,
    "createdAt": "2026-06-04T10:00:00",
    "updatedAt": "2026-06-04T10:00:00"
  }
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40002 | 指标配置不存在 |

---

#### 2.1.4 分页查询指标配置列表

**接口路径:** `GET /metrics/configs`

**接口含义:** 分页查询指标配置列表，支持按启用状态筛选和关键词搜索。

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| enabled | boolean | 否 | 筛选启用状态 | - |
| keyword | string | 否 | 搜索关键词（匹配指标名称） | - |
| pageNum | int | 否 | 页码，从1开始 | 1 |
| pageSize | int | 否 | 每页数量，范围1-100 | 20 |

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| list | array | 指标配置列表（元素为MetricConfigResponse） |
| total | long | 总记录数 |
| pageNum | int | 当前页码 |
| pageSize | int | 每页数量 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "metricName": "各城市素材数量",
        "enabled": true,
        "createdAt": "2026-06-04T10:00:00"
      }
    ],
    "total": 3,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

#### 2.1.5 启用/停用指标配置

**接口路径:** `PATCH /metrics/configs/{id}/enabled`

**接口含义:** 启用或停用指定的指标配置。停用的配置不能用于创建新任务。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 指标配置ID |

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| enabled | boolean | 是 | 启用状态：true启用，false停用 |

**请求示例:**
```json
{
  "enabled": false
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success"
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40002 | 指标配置不存在 |

---

#### 2.1.6 校验指标配置合法性

**接口路径:** `POST /metrics/configs/validate/{id}`

**接口含义:** 校验指定指标配置的合法性，包括表名、字段名、聚合方式、筛选条件等。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 指标配置ID |

**校验内容:**
1. source_table 是否在系统允许的表列表中（当前仅 asset）
2. field 和 group_by 是否是该表的合法字段
3. aggregation 与 field 类型是否兼容
4. filter_condition 是否有SQL注入风险

**响应示例:**
```json
{
  "code": 200,
  "message": "success"
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40002 | 指标配置不存在 |
| 40004 | 配置不合法（字段/表不存在） |
| 40005 | 聚合方式与字段类型不匹配 |

---

#### 2.1.7 删除指标配置

**接口路径:** `DELETE /metrics/configs/{id}`

**接口含义:** 删除指定的指标配置。已创建的任务不受影响。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 指标配置ID |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40002 | 指标配置不存在 |

---

### 2.2 任务接口

#### 2.2.1 创建查询任务

**接口路径:** `POST /tasks`

**接口含义:** 根据指定的指标配置创建异步查询任务，任务将进入消息队列等待执行。

**幂等性:** 若该指标存在 pending/running 状态的任务，直接返回原任务。

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| metricId | long | 是 | 指标配置ID |

**请求示例:**
```json
{
  "metricId": 1
}
```

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| taskId | string | 任务唯一标识（UUID） |
| metricId | long | 关联的指标配置ID |
| status | string | 任务状态：pending/running/success/failed |
| retryCount | int | 重试次数 |
| createdAt | string | 创建时间 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "metricId": 1,
    "status": "pending",
    "retryCount": 0,
    "createdAt": "2026-06-04T10:00:00"
  }
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40001 | 参数校验失败 |
| 40002 | 指标配置不存在 |
| 40007 | 任务仍在执行中（存在pending/running状态的任务） |
| 50002 | 消息队列异常 |

---

#### 2.2.2 查询任务状态和结果

**接口路径:** `GET /tasks/{taskId}`

**接口含义:** 根据任务ID查询任务的执行状态和结果数据。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID（创建任务时返回的UUID） |

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| taskId | string | 任务唯一标识 |
| metricId | long | 关联的指标配置ID |
| status | string | 任务状态 |
| startTime | string | 开始执行时间 |
| endTime | string | 结束时间 |
| resultData | array | 执行结果数据（success状态时有值） |
| errorMessage | string | 失败原因（failed状态时有值） |
| retryCount | int | 重试次数 |
| createdAt | string | 创建时间 |

**任务状态说明:**

| 状态 | 说明 |
|------|------|
| pending | 等待执行，任务已入队 |
| running | 执行中，正在查询数据 |
| success | 执行成功，结果数据已返回 |
| failed | 执行失败，errorMessage包含失败原因 |

**响应示例（进行中）:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "metricId": 1,
    "status": "running",
    "startTime": "2026-06-04T10:00:05",
    "retryCount": 0,
    "createdAt": "2026-06-04T10:00:00"
  }
}
```

**响应示例（成功）:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "metricId": 1,
    "status": "success",
    "startTime": "2026-06-04T10:00:05",
    "endTime": "2026-06-04T10:00:06",
    "retryCount": 0,
    "resultData": [
      {"city": "上海", "value": 15},
      {"city": "北京", "value": 12},
      {"city": "深圳", "value": 8}
    ],
    "createdAt": "2026-06-04T10:00:00"
  }
}
```

**响应示例（失败）:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "metricId": 1,
    "status": "failed",
    "startTime": "2026-06-04T10:00:05",
    "endTime": "2026-06-04T10:00:06",
    "retryCount": 3,
    "errorMessage": "SQL执行超时：查询超过30秒",
    "createdAt": "2026-06-04T10:00:00"
  }
}
```

**错误返回:**

| 错误码 | 说明 |
|--------|------|
| 40006 | 任务不存在 |

---

#### 2.2.3 分页查询任务列表

**接口路径:** `GET /tasks`

**接口含义:** 分页查询任务列表，支持按状态和指标ID筛选。

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| status | string | 否 | 筛选任务状态，可选值：pending/running/success/failed | - |
| metricId | long | 否 | 筛选指标配置ID | - |
| pageNum | int | 否 | 页码，从1开始 | 1 |
| pageSize | int | 否 | 每页数量，范围1-100 | 20 |

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| list | array | 任务列表（元素为TaskResponse） |
| total | long | 总记录数 |
| pageNum | int | 当前页码 |
| pageSize | int | 每页数量 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "taskId": "550e8400-e29b-41d4-a716-446655440000",
        "metricId": 1,
        "status": "success",
        "createdAt": "2026-06-04T10:00:00"
      }
    ],
    "total": 10,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

### 2.3 素材接口

#### 2.3.1 分页查询素材列表

**接口路径:** `GET /assets`

**接口含义:** 分页查询素材明细数据列表。

**查询参数:**

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| pageNum | int | 否 | 页码，从1开始 | 1 |
| pageSize | int | 否 | 每页数量，范围1-100 | 20 |

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| list | array | 素材列表（元素为Asset） |
| total | long | 总记录数 |
| pageNum | int | 当前页码 |
| pageSize | int | 每页数量 |

**list 数组中每个素材对象的字段:**

| 字段 | 类型 | 说明 |
|------|------|------|
| assetId | string | 素材ID |
| title | string | 素材标题 |
| uploader | string | 上传人 |
| uploadedAt | string | 上传时间 |
| fileSizeBytes | long | 文件大小（字节） |
| status | string | 审核状态：approved/rejected/pending |
| tags | string | 标签（逗号分隔） |
| city | string | 城市 |
| platform | string | 投放平台：抖音/快手/小红书 |
| durationSeconds | int | 视频时长（秒） |
| createdAt | string | 创建时间 |
| updatedAt | string | 更新时间 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "assetId": "A10001",
        "title": "春季活动短视频",
        "uploader": "张三",
        "uploadedAt": "2026-05-01T10:00:00",
        "fileSizeBytes": 104857600,
        "status": "approved",
        "tags": "活动,短视频",
        "city": "上海",
        "platform": "抖音",
        "durationSeconds": 35,
        "createdAt": "2026-05-01T10:00:00",
        "updatedAt": "2026-05-01T10:00:00"
      }
    ],
    "total": 30,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

#### 2.3.2 查询素材详情

**接口路径:** `GET /assets/{assetId}`

**接口含义:** 根据素材ID查询素材的详细信息。

**路径参数:**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| assetId | string | 是 | 素材ID |

**响应参数:** 返回单个素材的完整信息，字段同上。

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "assetId": "A10001",
    "title": "春季活动短视频",
    "uploader": "张三",
    "uploadedAt": "2026-05-01T10:00:00",
    "fileSizeBytes": 104857600,
    "status": "approved",
    "tags": "活动,短视频",
    "city": "上海",
    "platform": "抖音",
    "durationSeconds": 35,
    "createdAt": "2026-05-01T10:00:00",
    "updatedAt": "2026-05-01T10:00:00"
  }
}
```

---

#### 2.3.3 查询素材统计摘要

**接口路径:** `GET /assets/stats`

**接口含义:** 查询素材数据的统计摘要信息。

**响应参数:**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| total | long | 素材总数量 |

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 30
  }
}
```

---