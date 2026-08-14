package com.thx.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 守护各模块的<b>唯一初始化 SQL</b>。
 * <p>
 * 背景：项目原先同时存在两套建表脚本——各模块的 schema.sql（初始化用）
 * 和 {@code db/migration} 下的 Flyway 增量脚本，两者长期各自演进后发生了漂移：
 * schema.sql 曾缺失 {@code token_expire_time}、{@code relative_path_hash}、
 * {@code game_snapshot_root} 等由迁移脚本添加的结构，导致「新环境初始化出来的库」
 * 与「老环境迁移出来的库」结构不一致。
 * <p>
 * 2026-08 已把全部历史迁移合并回各模块 schema.sql，并约定：
 * <ul>
 *   <li>schema.sql 是该模块结构的<b>唯一事实来源</b>，始终代表最新结构；</li>
 *   <li>此后的结构变更走 Flyway（{@code db/migration}），<b>并同步更新 schema.sql</b>。</li>
 * </ul>
 * 本测试锁定那些容易在合并中被漏掉、且漏掉后果严重的结构点，防止再次漂移。
 * 断言失败通常意味着：改了 Flyway 脚本却忘了同步 schema.sql。
 */
class SchemaInitScriptTest {

    /**
     * 仓库根目录：模块化之后本测试位于 cms-app 模块，surefire 分叉 JVM 的 user.dir 是
     * 模块目录，而 docs/ 在聚合工程根，因此从 user.dir 逐级向上找 docs/modules。
     * 从 IDE、模块目录或聚合根运行都能命中。
     */
    private static final Path DOCS = locateDocsModules();

    private static Path locateDocsModules() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("docs").resolve("modules");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录下的 docs/modules（当前 user.dir="
                + System.getProperty("user.dir") + "）");
    }

    private static String read(String module, String file) throws IOException {
        Path path = DOCS.resolve(module).resolve(file);
        assertTrue(Files.exists(path), "初始化脚本缺失：" + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("每个模块有且只有一个初始化 SQL")
    void eachModuleHasExactlyOneInitScript() throws IOException {
        assertTrue(Files.exists(DOCS.resolve("platform").resolve("cms.sql")));
        assertTrue(Files.exists(DOCS.resolve("file").resolve("schema.sql")));
        assertTrue(Files.exists(DOCS.resolve("gamesave").resolve("schema.sql")));
        assertTrue(Files.exists(DOCS.resolve("payment").resolve("schema.sql")));

        try (var stream = Files.walk(DOCS)) {
            List<Path> sqlFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .toList();
            assertEquals4(sqlFiles);
        }
    }

    private static void assertEquals4(List<Path> sqlFiles) {
        assertTrue(sqlFiles.size() == 4,
                "docs/modules 下应恰好有 4 个初始化 SQL（每模块一个），实际为 " + sqlFiles.size()
                        + "：" + sqlFiles + "。新增结构变更请写 Flyway 脚本并同步对应 schema.sql，不要再新增独立 SQL 文件。");
    }

    @Test
    @DisplayName("gamesave schema 包含全部已合并的历史迁移结构")
    void gameSaveSchemaContainsMergedMigrations() throws IOException {
        String sql = read("gamesave", "schema.sql");

        // 原 V4：设备 Token 过期时间 + 唯一键由 device_id 改为 (user_id, device_id)
        assertTrue(sql.contains("token_expire_time"), "缺少 token_expire_time（原 V4）");
        assertTrue(sql.contains("uk_game_device_user_device"), "缺少 uk_game_device_user_device（原 V4）");
        assertFalse(sql.contains("uk_game_device_device_id"),
                "不应再有 uk_game_device_device_id：原 V4 已把它改成 (user_id, device_id) 联合唯一键");

        // 原 V5：用路径哈希做唯一约束，替代 512 前缀索引
        assertTrue(sql.contains("relative_path_hash"), "缺少 relative_path_hash（原 V5）");
        assertTrue(sql.contains("uk_game_snapshot_file_hash"), "缺少 uk_game_snapshot_file_hash（原 V5）");
        assertFalse(sql.contains("uk_game_snapshot_file_path"),
                "不应再有基于路径前缀的唯一键：原 V5 已改为对完整路径哈希做唯一约束");

        // 原 V6 + V8：异步清理任务表及其租约字段
        assertTrue(sql.contains("game_cleanup_task"), "缺少 game_cleanup_task（原 V6）");
        assertTrue(sql.contains("worker_id"), "缺少 worker_id（原 V8）");
        assertTrue(sql.contains("lease_until"), "缺少 lease_until（原 V8）");
        assertTrue(sql.contains("last_heartbeat_time"), "缺少 last_heartbeat_time（原 V8）");
        // 原 V8 还把清理索引的第三列由 create_time 改成 update_time
        assertTrue(sql.contains("`idx_game_object_cleanup` (`status`, `reference_count`, `update_time`)"),
                "idx_game_object_cleanup 第三列应为 update_time（原 V8 的修正）");

        // 原 V9：快照存档根路径元数据表
        assertTrue(sql.contains("game_snapshot_root"), "缺少 game_snapshot_root 表（原 V9）");
        assertTrue(sql.contains("include_patterns_json"), "缺少 include_patterns_json（原 V9）");

        // 原 V7：GameSave 接入文件模块的种子数据
        assertTrue(sql.contains("'game-save'"), "缺少 file_app 种子（原 V7）");
        assertTrue(sql.contains("'GAME_SAVE_OBJECT'"), "缺少 file_policy 种子（原 V7）");
        assertTrue(sql.contains("'save-object'"), "缺少 file_app_namespace 种子（原 V7）");
        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"), "种子数据必须幂等");
    }

    @Test
    @DisplayName("user.password 列宽足够存放 BCrypt（60 字符）")
    void userPasswordColumnFitsBcrypt() throws IOException {
        String sql = read("platform", "cms.sql");
        assertTrue(sql.contains("`password`        varchar(100)"),
                "user.password 必须至少 varchar(60)，当前脚本未使用扩宽后的列宽；"
                        + "BCrypt 哈希固定 60 字符，列宽不足会导致密文被截断、用户永久无法登录");
    }
}
