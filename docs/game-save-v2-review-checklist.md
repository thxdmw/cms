# GameSave V2 评审检查清单

- [ ] GitHub Actions `JDK 8 Maven 构建` 通过。
- [ ] 在一次性测试数据库中先执行 `docs/db/file_system.sql`，再执行 `docs/db/game_save.sql`。
- [ ] 确认 `game-save-private` Bucket 已存在，或由部署初始化流程创建。
- [ ] 注册后确认数据库只保存 PBKDF2 密码串，不保存明文密码。
- [ ] 登录轮换设备 Token 后，旧 Token 请求必须返回 401。
- [ ] 客户端不得出现 `X-File-Api-Key`。
- [ ] 同一用户重复内容对象只创建一个 `game_object`。
- [ ] 不同用户相同 SHA-256 内容不能互相复用 `OWNER_ONLY` 文件资产。
- [ ] 两个事务使用相同 expected HEAD 提交时，只允许一个推进 HEAD。
- [ ] CAS 失败事务中的 Snapshot、Manifest 和引用计数全部回滚。
- [ ] 客户端到 CMS 真实同步闭环完成集成验证。

稳定协议字段和错误码使用英文标识；Java 注释和 GameSave 文档统一使用中文。
