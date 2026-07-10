# GameSave V2 PR 摘要

本 PR 在现有 `module.file` 文件系统基建之上新增 `module.gamesave`，完成独立账号、设备 Token、云端逻辑游戏库、用户级内容对象去重、不可变 Snapshot Manifest 和同步 HEAD CAS。

核心安全边界没有改变：Windows 客户端只调用 `/api/game-save/v1/**`，禁止直接调用 `/api/v1/files`，禁止持有文件系统 App API Key。

所有 GameSave 新增代码注释和项目文档统一使用中文；协议字段、类名、数据库字段和稳定业务错误码保留英文标识。

当前 PR 仍保持 Draft。自动构建使用 JDK 8 + Maven；合并前还需要数据库初始化验证和并发 CAS 集成测试。
