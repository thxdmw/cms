package com.thx.module.gamesave.migration;

import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSaveMigrationResourceTest {

    @Test
    void filePolicySeedAndCleanupLeaseMigrationsArePackaged() throws Exception {
        String seed = resource("db/migration/V7__gamesave_file_policy_seed.sql");
        assertTrue(seed.contains("'game-save'"));
        assertTrue(seed.contains("'GAME_SAVE_OBJECT'"));
        assertTrue(seed.contains("'save-object'"));
        assertTrue(seed.contains("ON DUPLICATE KEY UPDATE"));

        String lease = resource("db/migration/V8__gamesave_cleanup_leases.sql");
        assertTrue(lease.contains("worker_id"));
        assertTrue(lease.contains("lease_until"));
        assertTrue(lease.contains("last_heartbeat_time"));
        assertTrue(lease.contains("update_time"));
    }

    private String resource(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "迁移资源未打入 classpath：" + path);
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        }
    }
}
