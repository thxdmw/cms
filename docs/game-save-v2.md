# GameSave V2 后端接入说明

## 当前范围

当前分支已经打通第一条云端同步闭环：

```text
注册/登录
    ↓
设备 Token
    ↓
创建或读取云端逻辑游戏
    ↓
读取云端 HEAD
    ↓
批量检查缺失内容对象
    ↓
上传缺失对象
    ↓
提交完整 Snapshot Manifest
    ↓
CAS 推进 game_sync_head
```

## 已实现

- `game_account`：独立 GameSave 用户账号。
- `game_device`：设备身份和设备 Token Hash。
- `game_library`：用户云端逻辑游戏。
- `game_object`：用户级 SHA-256 内容对象。
- `game_snapshot`：不可变快照。
- `game_snapshot_file`：快照完整文件清单。
- `game_sync_head`：每个用户、每个游戏唯一同步 HEAD。
- PBKDF2-SHA256 密码哈希。
- 设备 Token 注册、登录和轮换。
- 文件系统 `OWNER_ONLY` 内部身份桥接。
- 内容对象缺失检查、上传、服务端 SHA-256 二次校验和并发去重。
- 快照路径规范化、大小写不敏感判重和路径穿越拒绝。
- 新增/修改/删除文件变化数统计。
- Snapshot、Manifest、对象引用计数、HEAD CAS 同一 Spring 事务提交。

## 客户端安全边界

桌面客户端永远不接收 `X-File-Api-Key`，也不直接访问 `/api/v1/files`。

GameSave API 使用：

```http
Authorization: Bearer <device-token>
```

设备 Token 明文只在注册或登录成功响应中返回。服务端只保存 SHA-256 Hash。

## HEAD CAS

客户端提交：

```text
expectedHeadSnapshotId = 本机最后确认的云端 HEAD
```

服务端最终执行条件更新：

```text
UPDATE game_sync_head
SET head_snapshot_id = newHead,
    version = version + 1
WHERE user_id = 当前用户
  AND game_id = 当前游戏
  AND head_snapshot_id = expectedHead
```

更新行数为 0 时返回：

```text
HTTP 409
code = SYNC_CONFLICT
```

该异常触发事务回滚，已经插入的快照、Manifest 和对象引用计数不会残留。

## 尚未实现

- 云端 Snapshot 列表和时间线查询。
- Snapshot Manifest 下载接口。
- 客户端安全恢复事务。
- Snapshot 删除、引用计数递减和零引用对象释放。
- 游戏进程退出自动快照。
- 多设备冲突选择界面。
