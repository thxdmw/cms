# 文件系统模块（com.thx.module.file）

## 概述

`com.thx.module.file` 是一套服务多个应用（CMS、Agent 系统、Android App、宠物应用、图片生成应用、游戏等）复用的文件系统，
构建在当前 Spring Boot 单体项目内，底层使用 MinIO 作为对象存储。

核心只解决 5 个问题：

- **App 隔离**：每个接入方是一个独立的 `file_app`，文件的 `app_id` 只能来自认证上下文，不接受调用方传入，跨 App 一律返回 404
- **Policy 配置**：文件大小、扩展名、MIME、访问级别、Bucket 全部是数据库配置（`file_policy`），新增文件规则不需要改代码
- **Owner 权限**：`OWNER_ONLY` 级别的文件必须绑定 `ownerId`，只有对应用户才能访问
- **Object Storage 抽象**：业务代码不直接依赖 MinIO SDK，只依赖 `ObjectStorageClient` 接口，方便未来替换成 S3 / 阿里云 OSS
- **可靠删除和补偿**：删除先转为逻辑删除，宽限期后由定时任务物理清理，失败自动重试

本模块**不是**云厂商级对象存储平台，不做微服务拆分、不做复杂 IAM/ACL、不做 Kafka 事件、不做跨 App 去重，详见下方"当前不支持的能力"。

## 架构说明

### 调用链路

```text
外部 App（HTTP + API Key）          CMS 内部模块（Java 方法调用）
        │                                   │
        ▼                                   │
 FileController                             │
        │                                   │
        ▼                                   │
 FileAuthInterceptor                        │
   （查 file_app、校验 API Key、             │
    校验 @RequiredFileScope）                │
        │                                   │
        ▼                                   ▼
        └────────────► FileCallerContext ◄──┘
                              │
                              ▼
                     FileSystemService（门面）
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
      FilePolicyService  FileAuthService  ObjectStorageClient
      （查策略、校验文件） （权限校验）        （MinioObjectStorageClient）
              │               │                │
              ▼               ▼                ▼
          MySQL           MySQL             MinIO
                              │
                    FileCleanupService / FileCleanupTask
                    （逻辑删除 → 宽限期 → 物理清理 → 失败重试）
                              │
                       FileAuditService
                    （写 file_operation_log，失败不影响主流程）
```

外部应用通过 HTTP + API Key 调用；CMS 作为同进程内的可信调用方，直接构造
`FileCallerContext`（`CallerType.SYSTEM`）调用 `FileSystemService`，不经过
`FileAuthInterceptor`。无论哪条路径，`FileSystemService` 及以下各层都只依赖
`FileCallerContext`，不直接读取 `HttpServletRequest`/Header/Shiro Subject。

### Package 结构

```text
com.thx.module.file
├── annotation      RequiredFileScope（声明接口所需 Scope，未声明一律拒绝）
├── config          FileSystemProperties（跨 App 基础配置）、MinioClientConfig
├── context         FileCallerContext、FileCallerContextFactory
├── controller      FileController（/api/v1/files 对外 REST 接口）
├── enums           FileAccessLevel、FileStatus、CallerType、FileOperation
├── exception       FileSystemException、FileExceptionHandler
├── interceptor     FileAuthInterceptor（API Key 认证 + Scope 校验）
├── mapper          6 张表对应的 MyBatis-Plus Mapper
├── model           6 张表对应的实体类
├── service         FileSystemService（门面）+ FilePolicyService/FileAuthService/
│                   FileQuotaService/FileCleanupService/FileAuditService/FileUrlService
├── service.impl    上述接口的默认实现
├── storage         ObjectStorageClient、MinioObjectStorageClient、StorageHealthValidator
├── task            FileCleanupTask（定时物理清理 + 补偿任务重试）
├── util            ApiKeyUtil、FileHashUtil、MimeDetector、ObjectKeyGenerator、UrlJoinUtil
└── vo              FileUploadResult、FileInfoResult、FileUrlResult、ResponseVo
```

### 数据库（6 张表）

| 表 | 作用 |
| --- | --- |
| `file_app` | 接入方应用：`app_id`、`api_key_hash`（SHA-256，禁止明文）、`scopes`、`quota_bytes` |
| `file_policy` | 文件策略：大小/扩展名/MIME/访问级别/Bucket，纯数据配置，不支持脚本 |
| `file_app_namespace` | App 的业务场景（namespace）到 Policy 的映射，如 `(cms, article-image) -> PUBLIC_IMAGE` |
| `file_asset` | 文件元数据，只存 `storage_provider`/`bucket`/`object_key`，不存 URL，动态生成 |
| `storage_cleanup_task` | 物理删除失败后的补偿重试任务 |
| `file_operation_log` | 操作审计日志 |

建表和种子数据见 [docs/db/file_system.sql](../db/file_system.sql)。

## 接入方式（新增一个 App）

原则上**不需要改 Java 代码**，只需要插入数据：

### 1. 新增 App

```sql
-- 生成 API Key 明文（自己保存好，只告诉调用方），下面示例用 openssl 生成一个随机串
-- openssl rand -hex 24

-- 计算该明文的 SHA-256（十六进制），只保存哈希，不要保存明文
-- 例如用 Java：ApiKeyUtil.sha256Hex(apiKey)
-- 或者临时用命令行：echo -n '你的APIKey明文' | sha256sum

INSERT INTO `file_app` (`app_id`, `app_name`, `api_key_hash`, `scopes`, `quota_bytes`, `status`)
VALUES ('pet-app', '宠物应用', '<sha256(明文APIKey)>', 'UPLOAD,READ,DELETE,LIST,PRESIGN', NULL, 1);
```

`scopes` 是逗号分隔的 Scope 列表，可选值：`UPLOAD`、`READ`、`DELETE`、`LIST`、`PRESIGN`。

### 2. 复用已有 Policy，或新增一个

已有 `PUBLIC_IMAGE`（公开图片，10MB，jpg/jpeg/png/webp）和 `PRIVATE_FILE`（私有文件，100MB，仅限所有者）
两个策略可以直接复用。如果需要新的规则（比如游戏存档），插入一条新策略：

```sql
INSERT INTO `file_policy`
(`policy_code`, `max_file_size`, `allowed_extensions`, `allowed_mime_types`, `access_level`, `bucket`, `checksum_required`, `status`)
VALUES ('GAME_SAVE', 20971520, 'bin,dat,json', NULL, 'OWNER_ONLY', 'game-saves', 1, 1);
```

`access_level` 只能是 `PUBLIC` / `APP_INTERNAL` / `OWNER_ONLY` 三选一，未知值一律拒绝访问。
`bucket` 必须提前在 MinIO 里创建好（本地开发可以开 `storage.auto-create-bucket=true` 自动创建，生产环境禁止）。

### 3. 绑定 App 的业务场景（namespace）

```sql
INSERT INTO `file_app_namespace` (`app_id`, `namespace`, `policy_code`, `status`)
VALUES ('pet-app', 'avatar', 'PUBLIC_IMAGE', 1);
```

新增业务场景同理，`namespace` 只允许小写字母、数字、下划线、短横线。

完成以上 3 步，`pet-app` 就可以用 `avatar` 这个 namespace 上传公开图片了，全程不需要重新部署代码。

## 使用方式

### 外部应用：HTTP API

请求头固定为：

```text
X-File-App-Id: pet-app
X-File-Api-Key: <申请到的明文 API Key>
X-Request-Id: <可选，不传会自动生成，用于审计日志关联>
```

**上传文件**

```bash
curl -X POST http://localhost:8080/api/v1/files \
  -H "X-File-App-Id: pet-app" \
  -H "X-File-Api-Key: ${API_KEY}" \
  -F "file=@avatar.png" \
  -F "namespace=avatar"
# OWNER_ONLY 策略必须带上 ownerId，例如 -F "ownerId=user-123"
```

返回：

```json
{
  "status": 200,
  "msg": "操作成功",
  "data": {
    "fileId": "5ce489c3ed524b4484018a7dfb20c7a4",
    "originalName": "avatar.png",
    "url": "http://minio.example.com/public-assets/apps/pet-app/avatar/2026/07/05/5ce489c3ed524b4484018a7dfb20c7a4.png"
  }
}
```

**查询元数据**：`GET /api/v1/files/{fileId}`

**分页查询列表**：`GET /api/v1/files?page=1&size=10&namespace=avatar`

**获取下载 URL**（私有文件用，PUBLIC 文件直接用上传返回的 url 即可）：
`POST /api/v1/files/{fileId}/download-url`

**删除文件**：`DELETE /api/v1/files/{fileId}`（逻辑删除，立即返回 204，真正的物理删除由后台定时任务在宽限期后执行）

调用 `/api/v1/files/**` 下的任何接口，Controller 方法上必须有 `@RequiredFileScope`，
且该 App 的 `scopes` 必须包含对应值，否则一律 403（Fail Closed），不存在"没声明就放行"的情况。

### CMS 内部模块：直接调用 Java API

CMS 和文件模块在同一个 Spring Boot 进程里，不要绕一圈用 HTTP 调自己，直接注入
`FileSystemService`，用 `FileCallerContextFactory.forCms(userId, ip)` 构造调用方上下文：

```java
@RequiredArgsConstructor
public class SomeController {

    private final FileSystemService fileSystemService;

    public void upload(MultipartFile file, HttpServletRequest request) {
        String userId = currentUserId(); // 从 Shiro Subject 里取
        FileCallerContext caller = FileCallerContextFactory.forCms(userId, request.getRemoteAddr());

        // namespace=attachment 对应 PRIVATE_FILE 策略，OWNER_ONLY 必须传 ownerId
        FileUploadResult result = fileSystemService.upload(file, "attachment", userId, caller);

        // namespace=article-image 对应 PUBLIC_IMAGE 策略，公开图片不需要 ownerId
        // fileSystemService.upload(file, "article-image", null, caller);
    }
}
```

真实参考实现见 [UploadController](../../src/main/java/com/thx/module/admin/controller/UploadController.java)：
`/attachment/upload` 用 `attachment` namespace（私有附件，归属当前登录用户），
`/attachment/uploadForEditor` 用 `article-image` namespace（公开文章配图，无需 ownerId）。

## MINIO_ENDPOINT 与 MINIO_PUBLIC_DOMAIN：内网地址和外网地址必须分开配置

这是部署时最容易踩的坑：**MinIO SDK 生成 Presigned URL 时，URL 的 host 直接来自
构造 MinioClient 时用的那个 endpoint**，不是凭空拼出来的字符串。如果
`MINIO_ENDPOINT` 填的是内网地址（如 `http://localhost:20006`、容器内部地址、
仅内网可达的 IP），那么所有私有文件（`OWNER_ONLY`/`APP_INTERNAL`）的下载链接
都会带着这个内网地址，只有和 MinIO 在同一网络里的机器能打开，其他应用/浏览器/
手机 App 一律打不开图片。

本模块的做法是把两个地址拆开，对应两个用途：

| 配置项 | 谁在用 | 要求 |
| --- | --- | --- |
| `cms.file-system.storage.minio.endpoint`（`MINIO_ENDPOINT`） | 服务器自己：`putObject`/`removeObject`/启动时的 Bucket 存在性检查 | 只要服务器能访问到就行，内网地址、容器内地址都可以，追求速度 |
| `cms.file-system.public-domain`（`MINIO_PUBLIC_DOMAIN`） | 生成 PUBLIC 文件直链、生成私有文件 Presigned URL 的 host | 必须是最终打开链接的人（浏览器、其他 App、手机）能直接访问到的地址 |

对应实现在 [MinioObjectStorageClient](../../src/main/java/com/thx/module/file/storage/MinioObjectStorageClient.java)：
内部维护两个 `MinioClient`，一个用 `endpoint` 构造（给 `put`/`delete` 用），
另一个用 `public-domain` 构造（只给 `presignGet` 用），两者共用同一套
Access Key / Secret Key，构造过程本身不需要真的连上那个地址（生成 Presigned URL
只是本地做 HMAC 签名，不发请求）。如果 `public-domain` 没配置，会退化成直接
用内网 `endpoint`（并打印警告日志），仅适合调用方和 MinIO 确实在同一封闭网络、
不需要暴露给外部的场景。

**`MINIO_PUBLIC_DOMAIN` 具体填什么，取决于你的部署方式：**

1. **MinIO 有公网域名或公网 IP+端口**：直接填 `https://minio.example.com` 或
   `http://<公网IP>:9000`
2. **MinIO 只在内网，前面加了 Nginx/Caddy 反向代理对外暴露**：填反向代理对外的地址，
   例如 `https://files.example.com`。**反向代理必须原样转发请求的 Host 头**
   （nginx 例子：`proxy_set_header Host $host;`），因为 Presigned URL 的签名是
   基于 Host 计算的，如果代理把 Host 改写成内网地址再转发给 MinIO，MinIO 校验
   签名会失败，返回 403
3. **暂时没有对外可达的地址（比如本地开发、纯内网环境）**：留空或者直接和
   `MINIO_ENDPOINT` 填一样的值，私有文件的下载链接只有内网能打开，这是当前
   阶段唯一无法绕开的限制——要解决就必须先有一个外部能访问到 MinIO 的入口

## 安全设计要点

- API Key 只保存 SHA-256 哈希（`file_app.api_key_hash`），比较时使用 `ApiKeyUtil.matches()` 做恒定时间比较，不做明文存储/打印
- `FileAuthInterceptor` 认证成功后才会把 `FileCallerContext` 写入请求属性，Controller 方法若没有 `@RequiredFileScope` 一律拒绝
- `FileAuthServiceImpl` 校验读取/删除权限时，先做 App 隔离（跨 App 一律 404，不泄漏文件是否存在），再按访问级别放行；未知访问级别默认拒绝
- 上传时用 Apache Tika 检测文件真实内容类型（`detected_mime_type`），不完全信任声明的 Content-Type 和扩展名
- 上传使用 `DigestInputStream` 一次读取同时完成 SHA256 计算和 MinIO 上传，不重复读取文件
- 数据库写入失败会自动补偿删除已上传的 MinIO 对象；补偿删除也失败时登记 `storage_cleanup_task`（`CLEAN_ORPHAN`），交由定时任务重试，不会静默产生孤儿对象

## 当前不支持的能力（刻意不做）

以下能力都是刻意排除在当前阶段之外，避免过度设计：微服务拆分、Kafka/RabbitMQ 文件事件、Seata 分布式事务、
Redis 分布式锁、HMAC+Timestamp+Nonce 防重放、完整 ACL/文件分享、跨 App 去重、`file_app_usage` 配额账本、
Presigned PUT 直传、游戏存档 Revision/Conflict（未来 `com.thx.module.save` 会复用本模块的 `FileSystemService`，
但 Revision/CAS/冲突处理属于 save 模块自己的职责，文件模块只认识 `namespace=save` 这一个普通场景）。

如果未来确实需要以上某项能力，再单独评估设计，不要提前抽象。
