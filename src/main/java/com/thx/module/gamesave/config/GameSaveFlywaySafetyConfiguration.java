package com.thx.module.gamesave.config;

import org.flywaydb.core.api.FlywayException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/** 禁止 Flyway 自动猜测没有历史表的旧 GameSave 数据库版本。 */
@Configuration
public class GameSaveFlywaySafetyConfiguration {

    private static final List<String> GAME_SAVE_TABLES = Arrays.asList(
            "game_account", "game_device", "game_library", "game_object",
            "game_snapshot", "game_snapshot_file", "game_sync_head", "game_cleanup_task");

    @Bean
    public FlywayMigrationStrategy gameSaveFlywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            assertSafeToMigrate(dataSource);
            flyway.migrate();
        };
    }

    public static void assertSafeToMigrate(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (tableExists(connection, "flyway_schema_history")) return;
            for (String table : GAME_SAVE_TABLES) {
                if (tableExists(connection, table)) {
                    throw new FlywayException(
                            "检测到旧 GameSave 表但缺少 flyway_schema_history，已拒绝自动猜测版本。"
                                    + "请先备份数据库，执行 docs/modules/gamesave/migrate-legacy-non-flyway.sql，"
                                    + "再使用 Flyway 7.15 baselineVersion=8 建立基线后重启。");
                }
            }
        } catch (SQLException exception) {
            throw new FlywayException("检查旧 GameSave 数据库结构失败，已停止迁移。", exception);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            if (tables.next()) return true;
        }
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null,
                table.toUpperCase(java.util.Locale.ROOT), new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
