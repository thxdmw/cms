package com.thx.module.gamesave.config;

import org.flywaydb.core.api.FlywayException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
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
            boolean hasGameSaveTables = false;
            for (String table : GAME_SAVE_TABLES) {
                if (tableExists(connection, table)) {
                    hasGameSaveTables = true;
                    break;
                }
            }
            if (!hasGameSaveTables) return;
            if (tableExists(connection, "flyway_schema_history") && hasGameSaveHistory(connection)) return;
            throw new FlywayException(
                    "检测到 GameSave 表，但 flyway_schema_history 中没有可验证的 GameSave 迁移链"
                            + "或明确的‘旧 GameSave V8 人工基线’，已拒绝自动猜测版本。"
                            + "请先备份数据库，执行 docs/modules/gamesave/migrate-legacy-non-flyway.sql，"
                            + "再使用 Flyway 7.15 baselineVersion=8、baselineDescription=旧 GameSave V8 人工基线"
                            + "建立基线后重启。");
        } catch (SQLException exception) {
            throw new FlywayException("检查旧 GameSave 数据库结构失败，已停止迁移。", exception);
        }
    }

    private static boolean hasGameSaveHistory(Connection connection) throws SQLException {
        String directBaseline = "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND ("
                + "(version='1' AND LOWER(description)='gamesave baseline' AND `type`='SQL') OR "
                + "(version='8' AND description='旧 GameSave V8 人工基线' AND `type`='BASELINE'))";
        try (PreparedStatement statement = connection.prepareStatement(directBaseline);
             ResultSet result = statement.executeQuery()) {
            if (result.next() && result.getInt(1) > 0) return true;
        }

        // 兼容早期部署：它们在 V1 对既有 schema 做了普通基线，但随后真实执行了 V2..V8。
        // 必须逐条匹配版本、描述和 SQL 类型，不能因任意模块的历史表或普通 V1 baseline 放行。
        String verifiedLegacyChain = "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 "
                + "AND `type`='SQL' AND ((version='2' AND description='gamesave retention') "
                + "OR (version='3' AND description='gamesave object lifecycle') "
                + "OR (version='4' AND description='gamesave device token security') "
                + "OR (version='5' AND description='gamesave relative path hash') "
                + "OR (version='6' AND description='gamesave async game cleanup') "
                + "OR (version='7' AND description='gamesave file policy seed') "
                + "OR (version='8' AND description='gamesave cleanup leases'))";
        try (PreparedStatement statement = connection.prepareStatement(verifiedLegacyChain);
             ResultSet result = statement.executeQuery()) {
            return result.next() && result.getInt(1) == 7;
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
