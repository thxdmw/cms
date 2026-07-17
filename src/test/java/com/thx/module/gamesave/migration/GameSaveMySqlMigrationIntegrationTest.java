package com.thx.module.gamesave.migration;

import com.thx.module.gamesave.config.GameSaveFlywaySafetyConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 在 MySQL 5.7 上执行真实的空库迁移和 V6 旧数据升级。 */
@EnabledIfSystemProperty(named = "gamesave.integration", matches = "true")
class GameSaveMySqlMigrationIntegrationTest {

    private final String sourceUrl = requiredEnvironment("SPRING_DATASOURCE_URL");
    private final String username = requiredEnvironment("SPRING_DATASOURCE_USERNAME");
    private final String password = requiredEnvironment("SPRING_DATASOURCE_PASSWORD");

    @Test
    void emptyDatabaseMigratesFromV1ToV8() throws Exception {
        withDatabase("gamesave_empty_", databaseUrl -> {
            initializeFileSchema(databaseUrl);
            flyway(databaseUrl, null).migrate();
            JdbcTemplate jdbc = jdbc(databaseUrl);
            assertEquals("8", jdbc.queryForObject(
                    "SELECT version FROM flyway_schema_history WHERE success=1 ORDER BY installed_rank DESC LIMIT 1",
                    String.class));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM file_app WHERE app_id='game-save'"));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM file_policy WHERE policy_code='GAME_SAVE_OBJECT'"));
            assertNotNull(jdbc.queryForObject(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() "
                            + "AND table_name='game_cleanup_task' AND column_name='worker_id'", String.class));
        });
    }

    @Test
    void v6DataSurvivesUpgradeToV8() throws Exception {
        withDatabase("gamesave_upgrade_", databaseUrl -> {
            initializeFileSchema(databaseUrl);
            flyway(databaseUrl, MigrationVersion.fromVersion("6")).migrate();
            JdbcTemplate jdbc = jdbc(databaseUrl);
            insertV6Data(jdbc);
            flyway(databaseUrl, null).migrate();

            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM game_account WHERE user_id='legacy-user'"));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM game_library WHERE game_id='legacy-game'"));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM game_snapshot WHERE snapshot_id='legacy-snapshot'"));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM game_object WHERE object_id='legacy-object' "
                    + "AND status='ACTIVE' AND reference_count=1"));
            assertEquals(4096L, jdbc.queryForObject(
                    "SELECT used_bytes FROM game_account WHERE user_id='legacy-user'", Long.class).longValue());
            assertEquals("PENDING", jdbc.queryForObject(
                    "SELECT status FROM game_cleanup_task WHERE task_id='legacy-cleanup'", String.class));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM information_schema.statistics "
                    + "WHERE table_schema=DATABASE() AND table_name='game_object' "
                    + "AND index_name='idx_game_object_cleanup' AND column_name='update_time'"));
        });
    }

    @Test
    void legacySchemaWithoutFlywayHistoryRequiresExplicitV8Baseline() throws Exception {
        withDatabase("gamesave_legacy_", databaseUrl -> {
            initializeFileSchema(databaseUrl);
            executeScript(databaseUrl, "docs/modules/gamesave/schema.sql");
            JdbcTemplate jdbc = jdbc(databaseUrl);
            jdbc.update("INSERT INTO game_account(user_id,username,password_hash,quota_bytes,used_bytes,status) "
                    + "VALUES('manual-user','manual-name','hash',10737418240,1234,1)");

            DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseUrl, username, password);
            FlywayException refused = assertThrows(FlywayException.class,
                    () -> GameSaveFlywaySafetyConfiguration.assertSafeToMigrate(dataSource));
            assertTrue(refused.getMessage().contains("拒绝自动猜测版本"));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM game_account WHERE user_id='manual-user'"));

            executeScript(databaseUrl, "docs/modules/gamesave/migrate-legacy-non-flyway.sql");
            Flyway legacy = Flyway.configure().dataSource(databaseUrl, username, password)
                    .locations("classpath:db/migration")
                    .baselineVersion(MigrationVersion.fromVersion("8"))
                    .baselineDescription("旧 GameSave V8 人工基线")
                    .load();
            legacy.baseline();
            GameSaveFlywaySafetyConfiguration.assertSafeToMigrate(dataSource);
            legacy.migrate();

            assertEquals(1234L, jdbc.queryForObject(
                    "SELECT used_bytes FROM game_account WHERE user_id='manual-user'", Long.class).longValue());
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema=DATABASE() AND table_name='game_device' "
                    + "AND column_name='token_expire_time'"));
            assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema=DATABASE() AND table_name='game_snapshot_file' "
                    + "AND column_name='relative_path_hash'"));
        });
    }

    private void insertV6Data(JdbcTemplate jdbc) {
        String hash = repeat("a", 64);
        jdbc.update("INSERT INTO game_account(user_id,username,password_hash,quota_bytes,used_bytes,status) "
                + "VALUES('legacy-user','legacy-name','hash',10737418240,4096,1)");
        jdbc.update("INSERT INTO game_device(device_id,user_id,device_name,token_hash,token_expire_time,status) "
                + "VALUES('legacy-device','legacy-user','legacy','" + repeat("b", 64)
                + "',DATE_ADD(NOW(),INTERVAL 1 DAY),1)");
        jdbc.update("INSERT INTO game_library(game_id,user_id,game_key,name,provider,status) "
                + "VALUES('legacy-game','legacy-user','CUSTOM:legacy','Legacy Game','CUSTOM',1)");
        jdbc.update("INSERT INTO game_object(object_id,user_id,sha256,size,file_id,reference_count,status) "
                + "VALUES('legacy-object','legacy-user','" + hash + "',4096,'legacy-file',1,'ACTIVE')");
        jdbc.update("INSERT INTO game_snapshot(snapshot_id,user_id,game_id,device_id,trigger_type,file_count,"
                + "logical_size,changed_file_count,status) VALUES('legacy-snapshot','legacy-user','legacy-game',"
                + "'legacy-device','MANUAL',1,4096,1,'ACTIVE')");
        jdbc.update("INSERT INTO game_snapshot_file(snapshot_id,relative_path,relative_path_hash,object_id,size,sha256) "
                + "VALUES('legacy-snapshot','root1/save.dat','" + repeat("c", 64)
                + "','legacy-object',4096,'" + hash + "')");
        jdbc.update("INSERT INTO game_sync_head(user_id,game_id,head_snapshot_id,version) "
                + "VALUES('legacy-user','legacy-game','legacy-snapshot',1)");
        jdbc.update("INSERT INTO game_cleanup_task(task_id,user_id,game_id,status) "
                + "VALUES('legacy-cleanup','legacy-user','legacy-game','PENDING')");
    }

    private void initializeFileSchema(String databaseUrl) throws Exception {
        executeScript(databaseUrl, "docs/modules/file/schema.sql");
    }

    private void executeScript(String databaseUrl, String path) throws Exception {
        try (Connection connection = DriverManager.getConnection(databaseUrl, username, password)) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(path));
        }
    }

    private Flyway flyway(String databaseUrl, MigrationVersion target) {
        org.flywaydb.core.api.configuration.FluentConfiguration configuration = Flyway.configure()
                .dataSource(databaseUrl, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"));
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private JdbcTemplate jdbc(String databaseUrl) {
        return new JdbcTemplate(new DriverManagerDataSource(databaseUrl, username, password));
    }

    private int count(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private void withDatabase(String prefix, DatabaseWork work) throws Exception {
        String database = prefix + UUID.randomUUID().toString().replace("-", "");
        String serverUrl = sourceUrl.replaceFirst("/[^/?]+(\\?.*)?$", "/");
        try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database + " CHARACTER SET utf8mb4");
        }
        String databaseUrl = sourceUrl.replaceFirst("/[^/?]+(\\?.*)?$",
                "/" + database + (sourceUrl.contains("?") ? sourceUrl.substring(sourceUrl.indexOf('?')) : ""));
        try {
            work.run(databaseUrl);
        } finally {
            try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + database);
            }
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("缺少环境变量：" + name);
        return value;
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }

    private interface DatabaseWork {
        void run(String databaseUrl) throws Exception;
    }
}
