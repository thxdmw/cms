# Ops Agent API 接口文档

## 概述

本 CMS 项目为运维 Agent 提供了一套完整的监控和分析接口，包括：
- 系统资源监控（CPU、内存、磁盘、JVM、线程）
- 数据库性能监控（连接池、慢SQL、SQL统计）
- 日志分析（日志搜索、统计分析、错误日志）
- 综合健康检查

**基础路径**: `/agent/api/ops/`

**认证方式**: 所有接口都标注了 `@AnonymousAccess`，无需登录即可访问（但建议在生产环境配置 API Key 认证）

---

## 1. 系统资源监控接口

### 1.1 获取系统资源概览

**接口**: `GET /agent/api/ops/system/overview`

**描述**: 一次性获取 CPU、内存、磁盘、JVM、线程等所有系统资源信息

**响应示例**:
```json
{
  "code": 200,
  "message": "获取系统概览成功",
  "data": {
    "cpu": 45.67,
    "memory": {
      "totalMemory": "2.0 GB",
      "usedMemory": "1.2 GB",
      "freeMemory": "800.0 MB",
      "maxMemory": "4.0 GB",
      "usagePercent": 60.0,
      "heap": {
        "init": "256.0 MB",
        "used": "512.0 MB",
        "committed": "1.0 GB",
        "max": "2.0 GB"
      },
      "nonHeap": {
        "init": "2.4 MB",
        "used": "45.6 MB",
        "committed": "48.0 MB",
        "max": "-1 B"
      }
    },
    "disk": {
      "totalSpace": "100.0 GB",
      "usedSpace": "60.0 GB",
      "freeSpace": "40.0 GB",
      "usableSpace": "38.5 GB",
      "usagePercent": 60.0
    },
    "jvm": {
      "vmName": "Java HotSpot(TM) 64-Bit Server VM",
      "vmVersion": "25.291-b10",
      "javaVersion": "1.8.0_291",
      "vendor": "Oracle Corporation",
      "uptime": "2天 5小时 30分钟",
      "startTime": 1622073600000,
      "garbageCollectors": [
        {
          "name": "PS Scavenge",
          "collectionCount": 1234,
          "collectionTime": "5分钟 30秒"
        },
        {
          "name": "PS MarkSweep",
          "collectionCount": 56,
          "collectionTime": "2分钟 15秒"
        }
      ]
    },
    "threads": {
      "totalStartedThreadCount": 5678,
      "threadCount": 45,
      "peakThreadCount": 67,
      "daemonThreadCount": 23,
      "stateDistribution": {
        "NEW": 0,
        "RUNNABLE": 12,
        "BLOCKED": 2,
        "WAITING": 15,
        "TIMED_WAITING": 14,
        "TERMINATED": 2
      }
    },
    "systemInfo": {
      "osName": "Linux",
      "osVersion": "5.4.0-74-generic",
      "osArch": "amd64",
      "processors": "8",
      "systemLoadAverage": "3.45"
    }
  }
}
```

---

### 1.2 获取 CPU 使用率

**接口**: `GET /agent/api/ops/system/cpu`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取CPU使用率成功",
  "data": {
    "cpuUsage": 45.67,
    "unit": "%"
  }
}
```

---

### 1.3 获取内存使用情况

**接口**: `GET /agent/api/ops/system/memory`

**响应示例**: 见 1.1 中的 memory 字段

---

### 1.4 获取磁盘使用情况

**接口**: `GET /agent/api/ops/system/disk`

**响应示例**: 见 1.1 中的 disk 字段

---

### 1.5 获取 JVM 信息

**接口**: `GET /agent/api/ops/system/jvm`

**响应示例**: 见 1.1 中的 jvm 字段

---

### 1.6 获取线程信息

**接口**: `GET /agent/api/ops/system/threads`

**响应示例**: 见 1.1 中的 threads 字段

---

## 2. 数据库性能监控接口

### 2.1 获取数据库连接池概览

**接口**: `GET /agent/api/ops/database/overview`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取数据库概览成功",
  "data": {
    "activeCount": 5,
    "poolingCount": 15,
    "connectCount": 12345,
    "closeCount": 12340,
    "waitThreadCount": 0,
    "notEmptyWaitCount": 23,
    "notEmptyWaitMillis": 1234,
    "initialSize": 0,
    "minIdle": 3,
    "maxActive": 20,
    "maxWait": 10000,
    "usagePercent": 25.0,
    "sqlStatistics": {
      "executeCount": 98765,
      "errorCount": 12,
      "commitCount": 45678,
      "rollbackCount": 5
    }
  }
}
```

---

### 2.2 获取慢 SQL 列表

**接口**: `GET /agent/api/ops/database/slow-sql?topN=10`

**参数**:
- `topN`: 返回前 N 条最慢的 SQL（默认 10，最大 100）

**响应示例**:
```json
{
  "code": 200,
  "message": "获取慢SQL列表成功",
  "data": [
    {
      "sql": "SELECT * FROM biz_article WHERE status = ?",
      "executeCount": 1234,
      "totalTime": 45678,
      "avgTime": 37.02,
      "maxTime": 234,
      "errorCount": 0,
      "fetchRowCount": 5678,
      "inTransactionCount": 100
    },
    {
      "sql": "INSERT INTO biz_comment ...",
      "executeCount": 567,
      "totalTime": 12345,
      "avgTime": 21.77,
      "maxTime": 156,
      "errorCount": 2,
      "fetchRowCount": 0,
      "inTransactionCount": 567
    }
  ]
}
```

---

### 2.3 获取 SQL 执行统计

**接口**: `GET /agent/api/ops/database/sql-statistics`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取SQL统计成功",
  "data": {
    "executeCount": 98765,
    "errorCount": 12,
    "commitCount": 45678,
    "rollbackCount": 5,
    "errorRate": 0.01,
    "totalSqlTypes": 45,
    "mostFrequentSql": "SELECT * FROM biz_article WHERE status = ?",
    "mostFrequentSqlCount": 1234
  }
}
```

---

### 2.4 获取数据库基本信息

**接口**: `GET /agent/api/ops/database/info`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取数据库信息成功",
  "data": {
    "databaseProductName": "MySQL",
    "databaseProductVersion": "8.0.26",
    "driverName": "MySQL Connector/J",
    "driverVersion": "mysql-connector-j-8.0.33",
    "url": "jdbc:mysql://localhost:3306/cms",
    "userName": "root@localhost"
  }
}
```

---

## 3. 日志分析接口

### 3.1 获取日志统计信息

**接口**: `GET /agent/api/ops/log/statistics?startTime=2026-05-27 00:00&endTime=2026-05-27 23:59`

**参数**:
- `startTime`: 开始时间（可选，格式：yyyy-MM-dd HH:mm）
- `endTime`: 结束时间（可选，格式：yyyy-MM-dd HH:mm）

**响应示例**:
```json
{
  "code": 200,
  "message": "获取日志统计成功",
  "data": {
    "totalLogs": 15234,
    "errorCount": 123,
    "warnCount": 456,
    "infoCount": 14500,
    "debugCount": 155,
    "errorPercent": 0.81,
    "warnPercent": 2.99,
    "infoPercent": 95.18,
    "hourlyDistribution": {
      "00:00": 234,
      "01:00": 123,
      "02:00": 89,
      "...": "...",
      "23:00": 567
    },
    "errorTypes": {
      "NullPointerException": 45,
      "SQLException": 32,
      "TimeoutException": 23,
      "...": "..."
    },
    "recentErrors": [
      {
        "timestamp": "2026-05-27T10:30:45.123+08:00",
        "logger": "com.thx.module.admin.service.impl.BizArticleServiceImpl",
        "message": "查询文章失败: NullPointerException"
      }
    ]
  }
}
```

---

### 3.2 获取最近的错误日志

**接口**: `GET /agent/api/ops/log/recent-errors?count=10`

**参数**:
- `count`: 返回条数（默认 10，最大 100）

**响应示例**:
```json
{
  "code": 200,
  "message": "获取最近错误日志成功",
  "data": [
    {
      "timestamp": "2026-05-27T10:30:45.123+08:00",
      "logger": "com.thx.module.admin.service.impl.BizArticleServiceImpl",
      "message": "查询文章失败: NullPointerException\n\tat com.thx..."
    },
    {
      "timestamp": "2026-05-27T10:25:12.456+08:00",
      "logger": "com.thx.module.blog.controller.BlogController",
      "message": "处理请求异常: SQLException"
    }
  ]
}
```

---

### 3.3 搜索日志

**接口**: `GET /agent/api/ops/log/search?startTime=2026-05-27 00:00&endTime=2026-05-27 23:59&logLevel=ERROR&keyword=exception&page=1&size=50`

**参数**:
- `startTime`: 开始时间（可选）
- `endTime`: 结束时间（可选）
- `logLevel`: 日志级别（可选：INFO/WARN/ERROR/DEBUG）
- `keyword`: 关键词（可选）
- `page`: 页码（默认 1）
- `size`: 每页大小（默认 50）

**响应示例**:
```json
{
  "code": 200,
  "message": "搜索日志成功",
  "data": {
    "content": [
      {
        "id": null,
        "timestamp": "2026-05-27T10:30:45.123+08:00",
        "level": "ERROR",
        "logger": "com.thx.module.admin.service.impl.BizArticleServiceImpl",
        "message": "查询文章失败: NullPointerException"
      }
    ],
    "totalElements": 123,
    "totalPages": 3,
    "size": 50,
    "page": 1
  }
}
```

---

## 4. 综合健康检查

### 4.1 获取健康报告

**接口**: `GET /agent/api/ops/health/report`

**描述**: 一次性获取系统、数据库、日志的关键指标，并评估整体健康状态

**响应示例**:
```json
{
  "code": 200,
  "message": "获取健康报告成功",
  "data": {
    "system": {
      "cpu": 45.67,
      "memory": {...},
      "disk": {...},
      "jvm": {...},
      "threads": {...},
      "systemInfo": {...}
    },
    "database": {
      "activeCount": 5,
      "poolingCount": 15,
      "usagePercent": 25.0,
      ...
    },
    "logs": {
      "totalLogs": 15234,
      "errorCount": 123,
      "errorPercent": 0.81,
      ...
    },
    "healthStatus": "HEALTHY"
  }
}
```

**健康状态说明**:
- `HEALTHY`: 系统运行正常
- `WARNING`: 存在警告（如数据库连接池使用率 > 90% 或日志错误率 > 10%）
- `CRITICAL`: 严重问题（如 CPU 使用率 > 90% 或内存使用率 > 90%）
- `UNKNOWN`: 无法评估

---

## 使用建议

### 1. Agent 调用流程建议

```
1. 定期调用 /health/report 获取整体健康状况
2. 如果 healthStatus != "HEALTHY"，则深入调查：
   - CPU/内存高：调用 /system/overview 获取详细信息
   - 数据库问题：调用 /database/overview 和 /database/slow-sql
   - 错误增多：调用 /log/statistics 和 /log/recent-errors
3. 根据收集的数据生成分析报告
```

### 2. 推荐的监控频率

- **健康检查**: 每 5 分钟
- **系统资源**: 每 1-5 分钟
- **数据库性能**: 每 5-10 分钟
- **日志统计**: 每 10-30 分钟
- **慢 SQL**: 每小时或每天

### 3. 告警阈值建议

| 指标 | 警告阈值 | 严重阈值 |
|------|---------|---------|
| CPU 使用率 | > 70% | > 90% |
| 内存使用率 | > 80% | > 90% |
| 磁盘使用率 | > 80% | > 95% |
| 数据库连接池使用率 | > 70% | > 90% |
| 日志错误率 | > 5% | > 10% |
| SQL 错误率 | > 1% | > 5% |

---

## 注意事项

1. **性能影响**: 频繁调用监控接口可能会对系统性能产生轻微影响，建议合理设置调用频率
2. **数据精度**: CPU 使用率基于系统负载平均值计算，在 Windows 系统上可能不准确
3. **日志文件**: 日志统计需要读取日志文件，大量日志时可能耗时较长
4. **Druid 监控**: 确保 Druid 监控已启用（在 application.yml 中配置 `spring.datasource.druid.filters: stat,wall,slf4j`）
5. **安全考虑**: 生产环境建议添加 API Key 认证或 IP 白名单限制

---

## 更新日志

- **2026-05-27**: 初始版本，包含系统监控、数据库监控、日志分析、健康检查接口
