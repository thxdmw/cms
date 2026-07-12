# GameSave 端到端环境

此环境独立于默认 `docker-compose.yml`，仅用于本地验证，不占用开发环境常用端口。

## 启动

```powershell
docker compose -f docker-compose.gamesave-e2e.yml up --build -d
powershell -ExecutionPolicy Bypass -File scripts/e2e/gamesave-smoke.ps1
```

服务端口：CMS `18080`、MySQL `13306`、Redis `16379`、MinIO API `19000`、MinIO Console `19001`。

MySQL 初始化顺序为 `cms.sql`、`file_system.sql`、`game_save.sql`。Compose 中的数据库和 MinIO 密钥只用于本地测试，禁止用于任何其他环境。

## 冒烟范围

脚本创建随机用户和两个设备，验证：

1. 注册、登录和设备 Token；
2. 内容对象 SHA-256 校验上传；
3. 首个不可变快照；
4. 陈旧 HEAD 的 `SYNC_CONFLICT`；
5. 第二个快照、时间线和完整 Manifest；
6. 预签名下载与 SHA-256 校验。

## 清理

```powershell
docker compose -f docker-compose.gamesave-e2e.yml down -v
```

`-v` 会删除该端到端环境的 MySQL 与 MinIO 测试数据。