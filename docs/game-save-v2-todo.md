# GameSave V2 后续任务

## 高优先级

- 增加 Snapshot 时间线分页查询。
- 增加 Snapshot Manifest 查询和内容对象下载 URL 聚合接口。
- 实现 Snapshot 删除、对象引用计数递减和零引用文件生命周期释放。
- 增加设备撤销和全部设备 Token 失效能力。
- 增加账号登录限流和失败次数控制。

## 测试

- 重复内容对象并发上传测试。
- 两台设备同时使用相同 expected HEAD 提交快照的 CAS 冲突测试。
- Manifest 重复路径、大小写路径冲突和 `..` 路径穿越测试。
- Snapshot 事务回滚后引用计数一致性测试。
- 设备 Token 轮换后旧 Token 失效测试。

## 后续产品能力

- Steam/GOG/Epic 游戏元数据适配。
- Snapshot 保留策略。
- 存储配额和用户逻辑容量统计。
- 多设备冲突人工选择流程。

## 代码规范

GameSave 新增 Java 注释和专用文档统一使用中文；代码标识符、数据库字段、HTTP Header 和稳定业务错误码继续使用英文。
