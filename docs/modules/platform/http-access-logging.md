# HTTP 接口日志

系统会为每个动态 HTTP 请求生成或继承 `X-Request-Id`，并把它：

- 写入响应头，方便前端或调用方反馈问题时提供；
- 放入 MDC，使同一请求里的业务日志、异常日志具有相同 `traceId`；
- 写入结构化访问日志，便于按字段检索。

## 日志位置

- `logs/cms.log`：应用全量日志；
- `logs/http-access.log`：HTTP 结构化访问日志；
- `logs/error.log`：仅 ERROR 日志和异常栈；
- `logs/archive/`：按日期和文件大小压缩归档，默认保留 30 天。

访问日志示例：

```json
{
  "event": "http_access",
  "traceId": "client-request-123",
  "method": "POST",
  "path": "/api/orders",
  "route": "/api/orders",
  "handler": "OrderController#create",
  "status": 200,
  "durationMs": 18,
  "slow": false,
  "clientIp": "127.0.0.1",
  "caller": "admin",
  "headers": {
    "content-type": "application/json"
  },
  "query": {
    "page": ["1"],
    "token": ["***"]
  },
  "requestBytes": 37,
  "responseBytes": 128,
  "requestBody": {
    "product": "book",
    "password": "***"
  }
}
```

## 安全策略

- 只记录白名单请求头，不记录 `Authorization`、`Cookie` 和 API Key；
- JSON、表单和查询参数中的密码、Token、Secret 等字段递归脱敏；
- 正文超过限制时整段省略，不记录可能无法完整解析的片段；
- 文件上传、下载等二进制内容不写正文，只记录字节数；
- 开发环境默认记录脱敏后的请求/响应正文，生产环境默认关闭正文。

## 配置

```yaml
cms:
  http-logging:
    enabled: true
    include-request-body: false
    include-response-body: false
    max-payload-length: 4096
    slow-request-threshold-ms: 1000
    excluded-paths:
      - /favicon.ico
      - /static/**
```

线上临时开启正文日志时应设置较短的观察窗口，并在排查完成后关闭。
