package com.thx.module.gamesave.integration;

import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 MySQL 行锁验证对象状态机和清理租约的并发不变量。 */
@SpringBootTest
@EnabledIfSystemProperty(named = "gamesave.integration", matches = "true")
class GameSaveConcurrencyIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private GameObjectMapper objectMapper;
    @Autowired private GameCleanupTaskMapper cleanupTaskMapper;

    @Test
    void checkMissingTouchAndOrphanClaimCannotCrossStateBoundary() throws Exception {
        Fixture fixture = fixture("touch-claim");
        jdbc.update("INSERT INTO game_object(object_id,user_id,sha256,size,file_id,reference_count,status,update_time) "
                        + "VALUES(?,?,?,?,?,0,'ACTIVE',DATE_SUB(NOW(),INTERVAL 1 DAY))",
                fixture.objectId, fixture.userId, hash('a'), 10L, "file-" + fixture.suffix);
        Long id = jdbc.queryForObject("SELECT id FROM game_object WHERE object_id=?", Long.class, fixture.objectId);

        int[] updates = concurrently(
                () -> objectMapper.touchUnreferencedActiveObjects(
                        fixture.userId, Collections.singletonList(id)),
                () -> objectMapper.markOrphanDeleting(fixture.objectId, fixture.userId, new Date()));

        String status = jdbc.queryForObject("SELECT status FROM game_object WHERE object_id=?", String.class,
                fixture.objectId);
        assertEquals(1, updates[0] + updates[1], "触碰与孤儿抢占只能有一个跨越旧状态成功");
        assertTrue("ACTIVE".equals(status) || "DELETING".equals(status));
    }

    @Test
    void snapshotReferenceAndDeletingClaimCannotBothSucceed() throws Exception {
        Fixture fixture = fixture("reference-claim");
        jdbc.update("INSERT INTO game_object(object_id,user_id,sha256,size,file_id,reference_count,status) "
                        + "VALUES(?,?,?,?,?,0,'ACTIVE')",
                fixture.objectId, fixture.userId, hash('b'), 20L, "file-" + fixture.suffix);

        int[] updates = concurrently(
                () -> objectMapper.incrementReference(fixture.objectId, fixture.userId),
                () -> objectMapper.markDeletingIfUnreferenced(fixture.objectId, fixture.userId));

        assertEquals(1, updates[0] + updates[1], "增加快照引用与 DELETING 抢占只能有一个成功");
        String state = jdbc.queryForObject(
                "SELECT CONCAT(status,':',reference_count) FROM game_object WHERE object_id=?",
                String.class, fixture.objectId);
        assertTrue("ACTIVE:1".equals(state) || "DELETING:0".equals(state));
    }

    @Test
    void cleanupCompletionAndReactivationRemainLinearizable() throws Exception {
        Fixture fixture = fixture("delete-reactivate");
        jdbc.update("INSERT INTO game_object(object_id,user_id,sha256,size,file_id,reference_count,status) "
                        + "VALUES(?,?,?,?,?,0,'DELETING')",
                fixture.objectId, fixture.userId, hash('c'), 30L, "old-file-" + fixture.suffix);
        Long id = jdbc.queryForObject("SELECT id FROM game_object WHERE object_id=?", Long.class, fixture.objectId);

        int[] updates = concurrently(
                () -> objectMapper.markDeletedFromDeleting(fixture.objectId, fixture.userId),
                () -> objectMapper.reactivateDeleted(id, fixture.userId, "new-file-" + fixture.suffix));

        assertEquals(1, updates[0], "DELETING 对象必须由清理完成方转换为 DELETED");
        assertTrue(updates[1] == 0 || updates[1] == 1);
        String state = jdbc.queryForObject(
                "SELECT CONCAT(status,':',reference_count) FROM game_object WHERE object_id=?",
                String.class, fixture.objectId);
        assertTrue("DELETED:0".equals(state) || "ACTIVE:0".equals(state));
    }

    @Test
    void expiredCleanupLeaseCanOnlyBeClaimedByOneWorker() throws Exception {
        Fixture fixture = fixture("lease");
        String gameId = "game-" + fixture.suffix;
        String taskId = "task-" + fixture.suffix;
        jdbc.update("INSERT INTO game_library(game_id,user_id,game_key,name,provider,status) "
                        + "VALUES(?,?,?,?, 'CUSTOM',2)",
                gameId, fixture.userId, "CUSTOM:" + fixture.suffix, "并发租约验证");
        jdbc.update("INSERT INTO game_cleanup_task(task_id,user_id,game_id,status,worker_id,lease_until) "
                        + "VALUES(?,?,?,'RUNNING','expired-worker',DATE_SUB(NOW(),INTERVAL 1 MINUTE))",
                taskId, fixture.userId, gameId);

        int[] updates = concurrently(
                () -> cleanupTaskMapper.claim(taskId, "worker-a", 120),
                () -> cleanupTaskMapper.claim(taskId, "worker-b", 120));

        assertEquals(1, updates[0] + updates[1], "同一过期租约只能被一个 Worker 认领");
        String worker = jdbc.queryForObject(
                "SELECT worker_id FROM game_cleanup_task WHERE task_id=?", String.class, taskId);
        assertTrue("worker-a".equals(worker) || "worker-b".equals(worker));
    }

    private Fixture fixture(String purpose) {
        String suffix = purpose + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String userId = "user-" + suffix;
        jdbc.update("INSERT INTO game_account(user_id,username,password_hash,quota_bytes,used_bytes,status) "
                        + "VALUES(?,?,?,10737418240,0,1)",
                userId, "name-" + suffix, "hash");
        return new Fixture(suffix, userId, "object-" + suffix);
    }

    private int[] concurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> firstResult = executor.submit(() -> awaitAndRun(ready, start, first));
            Future<Integer> secondResult = executor.submit(() -> awaitAndRun(ready, start, second));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "并发数据库操作未能同时就绪");
            start.countDown();
            return new int[]{firstResult.get(30, TimeUnit.SECONDS), secondResult.get(30, TimeUnit.SECONDS)};
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private int awaitAndRun(CountDownLatch ready, CountDownLatch start, Callable<Integer> operation)
            throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("等待并发数据库屏障超时");
        return operation.call();
    }

    private String hash(char value) {
        StringBuilder result = new StringBuilder(64);
        for (int index = 0; index < 64; index++) result.append(value);
        return result.toString();
    }

    private static final class Fixture {
        private final String suffix;
        private final String userId;
        private final String objectId;

        private Fixture(String suffix, String userId, String objectId) {
            this.suffix = suffix;
            this.userId = userId;
            this.objectId = objectId;
        }
    }
}
