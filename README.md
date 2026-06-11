# 指标配置后台服务

面向业务人员使用的「指标配置后台服务」，让业务人员可以通过后台页面配置常用指标（视频素材经营分析场景）。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS 版本，性能提升 |
| Spring Boot | 3.4.5 | Web框架 |
| MyBatis | 3.0.3 | 持久层框架 |
| MySQL | 8.0 | 业务数据库 |
| RabbitMQ | 3.x | 消息队列 |
| Maven | 3.6+ | 构建工具 |

## 项目结构

```
metrics-config-service/
├── pom.xml                          # 父工程pom
├── app/
│   ├── pom.xml                      # 子模块pom
│   └── src/main/
│       ├── java/com/example/metrics/
│       │   ├── config/              # 配置类（RabbitMQ、异步任务、Web配置）
│       │   ├── controller/          # REST接口
│       │   ├── service/             # 业务逻辑
│       │   ├── mapper/              # MyBatis Mapper
│       │   ├── model/               # 数据模型
│       │   │   ├── entity/          # 实体类
│       │   │   ├── dto/             # 数据传输对象
│       │   │   └── enums/           # 枚举类
│       │   ├── exception/           # 异常处理
│       │   └── util/                # 工具类（SQL构建、字段校验）
│       └── resources/
│           ├── application.yml      # 应用配置
│           ├── schema.sql           # 建表脚本
│           ├── data.sql             # 初始化数据（30条素材 + 3条预置指标）
│           └── mapper/              # MyBatis XML映射文件
└── deploy/                          # 部署配置
```

## 环境要求

- JDK 21
- MySQL 8.0
- RabbitMQ 3.x
- Maven 3.6+

## 快速开始

### 1. 启动依赖服务

```bash
# 启动 MySQL
mysql.server start  # macOS
# 或
systemctl start mysql  # Linux

# 启动 RabbitMQ
rabbitmq-server
```

### 2. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS metrics_db DEFAULT CHARSET utf8mb4;
```

### 3. 编译运行

```bash
# 进入项目目录
cd metrics-config-service

# 编译
mvn clean package -DskipTests

# 运行
java -jar app/target/app-1.0.0.jar

# 或开发模式运行
mvn spring-boot:run -pl app
```

### 4. 验证启动

服务启动后自动执行建表和初始化数据脚本，访问：
```
http://localhost:8080/api/v1/assets/stats
```

返回示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 30
  }
}
```

## 配置文件

应用配置：`app/src/main/resources/application.yml`

关键配置项：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/metrics_db
    username: root
    password: root
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

mybatis:
  configuration:
    default-statement-timeout: 30  # SQL超时时间（秒）
```

## API接口

### 指标配置接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/metrics/configs | 新增指标配置 |
| PUT | /api/v1/metrics/configs/{id} | 更新指标配置 |
| GET | /api/v1/metrics/configs/{id} | 获取指标配置详情 |
| GET | /api/v1/metrics/configs | 获取指标配置列表（分页） |
| PATCH | /api/v1/metrics/configs/{id}/enabled | 启用/停用指标配置 |
| POST | /api/v1/metrics/configs/validate/{id} | 校验指标配置合法性 |
| DELETE | /api/v1/metrics/configs/{id} | 删除指标配置 |

### 任务接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/tasks | 创建查询任务（异步） |
| GET | /api/v1/tasks/{taskId} | 获取任务状态和结果 |
| GET | /api/v1/tasks | 获取任务列表（分页） |

### 素材接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/assets | 获取素材列表（分页） |
| GET | /api/v1/assets/{assetId} | 获取素材详情 |
| GET | /api/v1/assets/stats | 获取素材统计摘要 |

## 测试说明

可以使用 curl 或 Postman 进行接口测试。

### 创建指标配置

```bash
curl -X POST http://localhost:8080/api/v1/metrics/configs \
  -H "Content-Type: application/json" \
  -d '{
    "metricName": "各平台素材数量",
    "description": "统计各投放平台的素材数量",
    "sourceTable": "asset",
    "field": "asset_id",
    "aggregation": "COUNT",
    "groupBy": "platform",
    "sortRule": "value DESC"
  }'
```

### 创建查询任务

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"metricId": 1}'
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "metricId": 1,
    "status": "pending",
    "retryCount": 0,
    "createdAt": "2026-06-11T10:00:00"
  }
}
```

### 查询任务状态

```bash
curl http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000
```

响应（成功）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "metricId": 1,
    "status": "success",
    "startTime": "2026-06-11T10:00:01",
    "endTime": "2026-06-11T10:00:02",
    "resultData": [
      {"platform": "抖音", "value": 12},
      {"platform": "快手", "value": 10},
      {"platform": "小红书", "value": 8}
    ],
    "createdAt": "2026-06-11T10:00:00"
  }
}
```

## 预置指标

系统初始化了3个预置指标：

| ID | 指标名称 | 说明 |
|----|----------|------|
| 1 | 按审核状态统计素材数量 | 统计各审核状态的素材数量 |
| 2 | 各上传人平均文件大小 | 统计已通过审核素材中，各上传人的平均文件大小 |
| 3 | 各城市素材数量 | 统计各城市的素材数量分布 |

快速测试命令：
```bash
# 创建任务（使用预置指标1）
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"metricId": 1}'

# 等待2秒后查询结果
curl http://localhost:8080/api/v1/tasks/{taskId}
```


