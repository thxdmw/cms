package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.util.UUIDUtil;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.SnapshotCommitRequest;
import com.thx.module.gamesave.dto.SnapshotCommitResult;
import com.thx.module.gamesave.dto.SnapshotFileDescriptor;
import com.thx.module.gamesave.dto.SyncHeadResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.model.GameSnapshotFile;
import com.thx.module.gamesave.model.GameSyncHead;
import com.thx.module.gamesave.service.GameObjectService;
import com.thx.module.gamesave.service.GameSnapshotService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** GameSave 不可变快照事务实现。 */
@Service
@RequiredArgsConstructor
public class GameSnapshotServiceImpl implements GameSnapshotService {
    @Override
    public List<com.thx.module.gamesave.dto.SnapshotSummaryResult> listSnapshots(
            String gameId, int limit, GameCallerContext caller) {
        requireOwnedGame(gameId, caller.getUserId());
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<GameSnapshot> snapshots = gameSnapshotMapper.selectList(
                new LambdaQueryWrapper<GameSnapshot>()
                        .eq(GameSnapshot::getGameId, gameId.trim())
                        .eq(GameSnapshot::getUserId, caller.getUserId())
                        .eq(GameSnapshot::getStatus, ACTIVE)
                        .orderByDesc(GameSnapshot::getCreateTime)
                        .last("LIMIT " + safeLimit));
        List<com.thx.module.gamesave.dto.SnapshotSummaryResult> results =
                new ArrayList<>(snapshots.size());
        for (GameSnapshot snapshot : snapshots) {
            results.add(com.thx.module.gamesave.dto.SnapshotSummaryResult.from(snapshot));
        }
        return results;
    }
    @Override
    public com.thx.module.gamesave.dto.SnapshotManifestResult getSnapshot(
            String gameId,
            String snapshotId,
            GameCallerContext caller) {
        requireOwnedGame(gameId, caller.getUserId());
        if (snapshotId == null || snapshotId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_SNAPSHOT_ID", "快照 ID 不能为空");
        }

        GameSnapshot snapshot = gameSnapshotMapper.selectOne(new LambdaQueryWrapper<GameSnapshot>()
                .eq(GameSnapshot::getSnapshotId, snapshotId.trim())
                .eq(GameSnapshot::getGameId, gameId.trim())
                .eq(GameSnapshot::getUserId, caller.getUserId())
                .eq(GameSnapshot::getStatus, ACTIVE)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw GameSaveException.notFound("SNAPSHOT_NOT_FOUND", "快照不存在或无权访问");
        }

        List<GameSnapshotFile> snapshotFiles = gameSnapshotFileMapper.selectList(
                new LambdaQueryWrapper<GameSnapshotFile>()
                        .eq(GameSnapshotFile::getSnapshotId, snapshot.getSnapshotId())
                        .orderByAsc(GameSnapshotFile::getRelativePath));
        List<com.thx.module.gamesave.dto.SnapshotManifestFileResult> files =
                new ArrayList<>(snapshotFiles.size());
        for (GameSnapshotFile file : snapshotFiles) {
            files.add(new com.thx.module.gamesave.dto.SnapshotManifestFileResult(
                    file.getRelativePath(),
                    file.getObjectId(),
                    file.getSha256(),
                    file.getSize()));
        }
        return new com.thx.module.gamesave.dto.SnapshotManifestResult(
                snapshot.getSnapshotId(),
                snapshot.getGameId(),
                snapshot.getDeviceId(),
                snapshot.getParentSnapshotId(),
                snapshot.getTriggerType(),
                snapshot.getDescription(),
                snapshot.getCreateTime(),
                files);
    }

    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> ALLOWED_TRIGGER_TYPES = new HashSet<>(
            Arrays.asList("MANUAL", "GAME_EXIT", "BEFORE_RESTORE", "IMPORT"));

    private final GameLibraryMapper gameLibraryMapper;
    private final GameSnapshotMapper gameSnapshotMapper;
    private final GameSnapshotFileMapper gameSnapshotFileMapper;
    private final GameSyncHeadMapper gameSyncHeadMapper;
    private final GameObjectMapper gameObjectMapper;
    private final GameObjectService gameObjectService;

    @Override
    public SyncHeadResult getHead(String gameId, GameCallerContext caller) {
        requireOwnedGame(gameId, caller.getUserId());
        GameSyncHead head = ensureAndGetHead(caller.getUserId(), gameId);
        return new SyncHeadResult(gameId, head.getHeadSnapshotId(), head.getVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SnapshotCommitResult commit(String gameId,
                                       SnapshotCommitRequest request,
                                       GameCallerContext caller) {
        requireOwnedGame(gameId, caller.getUserId());
        validateRequest(request);

        GameSyncHead currentHead = ensureAndGetHead(caller.getUserId(), gameId);
        String expectedHead = normalizeNullableId(request.getExpectedHeadSnapshotId());
        if (!Objects.equals(currentHead.getHeadSnapshotId(), expectedHead)) {
            throw GameSaveException.conflict("SYNC_CONFLICT", "云端存档已被其他设备更新，请先处理同步冲突");
        }

        List<ResolvedSnapshotFile> resolvedFiles = resolveManifest(request.getFiles(), caller);
        long logicalSize = calculateLogicalSize(resolvedFiles);
        int changedFileCount = calculateChangedFileCount(currentHead.getHeadSnapshotId(), resolvedFiles);

        // 已有 HEAD 且 Manifest 完全一致时直接返回当前版本，避免重复同步制造零变化快照、
        // 重复 snapshot_file 元数据以及无意义的对象引用计数增长。
        if (currentHead.getHeadSnapshotId() != null && changedFileCount == 0) {
            GameSyncHead confirmedHead = findHead(caller.getUserId(), gameId);
            if (confirmedHead == null
                    || !Objects.equals(currentHead.getHeadSnapshotId(), confirmedHead.getHeadSnapshotId())
                    || !Objects.equals(currentHead.getVersion(), confirmedHead.getVersion())) {
                throw GameSaveException.conflict("SYNC_CONFLICT", "云端存档已被其他设备更新，请先处理同步冲突");
            }
            return new SnapshotCommitResult(
                    currentHead.getHeadSnapshotId(),
                    currentHead.getVersion(),
                    resolvedFiles.size(),
                    logicalSize,
                    0,
                    false);
        }

        String snapshotId = UUIDUtil.uuid();
        GameSnapshot snapshot = new GameSnapshot()
                .setSnapshotId(snapshotId)
                .setUserId(caller.getUserId())
                .setGameId(gameId)
                .setDeviceId(caller.getDeviceId())
                .setParentSnapshotId(currentHead.getHeadSnapshotId())
                .setTriggerType(request.getTriggerType().trim().toUpperCase(Locale.ROOT))
                .setDescription(normalizeDescription(request.getDescription()))
                .setFileCount(resolvedFiles.size())
                .setLogicalSize(logicalSize)
                .setChangedFileCount(changedFileCount)
                .setStatus(ACTIVE);
        gameSnapshotMapper.insert(snapshot);

        for (ResolvedSnapshotFile resolved : resolvedFiles) {
            GameSnapshotFile snapshotFile = new GameSnapshotFile()
                    .setSnapshotId(snapshotId)
                    .setRelativePath(resolved.getRelativePath())
                    .setObjectId(resolved.getObject().getObjectId())
                    .setSize(resolved.getObject().getSize())
                    .setSha256(resolved.getObject().getSha256());
            gameSnapshotFileMapper.insert(snapshotFile);

            int updated = gameObjectMapper.incrementReference(
                    resolved.getObject().getObjectId(), caller.getUserId());
            if (updated != 1) {
                throw GameSaveException.conflict("OBJECT_STATE_CHANGED", "快照引用的内容对象状态已变化，请重新同步");
            }
        }

        int advanced = gameSyncHeadMapper.advanceHeadCas(
                caller.getUserId(), gameId, expectedHead, snapshotId);
        if (advanced != 1) {
            // 该异常为 RuntimeException，Spring 会回滚前面的快照、Manifest 和引用计数更新。
            throw GameSaveException.conflict("SYNC_CONFLICT", "云端存档已被其他设备更新，请先处理同步冲突");
        }

        GameSyncHead committedHead = findHead(caller.getUserId(), gameId);
        return new SnapshotCommitResult(
                snapshotId,
                committedHead.getVersion(),
                resolvedFiles.size(),
                logicalSize,
                changedFileCount,
                true);
    }

    /** 将客户端 Manifest 解析为当前用户实际拥有的内容对象，并完成路径规范化与判重。 */
    private List<ResolvedSnapshotFile> resolveManifest(List<SnapshotFileDescriptor> files,
                                                       GameCallerContext caller) {
        if (files == null) {
            throw GameSaveException.badRequest("INVALID_MANIFEST", "快照文件清单不能为空");
        }

        Map<String, ResolvedSnapshotFile> uniquePaths = new LinkedHashMap<>();
        for (SnapshotFileDescriptor descriptor : files) {
            if (descriptor == null) {
                throw GameSaveException.badRequest("INVALID_MANIFEST_FILE", "快照文件描述不能为空");
            }
            String relativePath = normalizeRelativePath(descriptor.getPath());
            String pathKey = relativePath.toLowerCase(Locale.ROOT);
            if (uniquePaths.containsKey(pathKey)) {
                throw GameSaveException.badRequest("DUPLICATE_PATH", "快照中存在重复路径: " + relativePath);
            }

            GameObject object = gameObjectService.requireOwnedObject(
                    descriptor.getSha256(), descriptor.getSize(), caller);
            uniquePaths.put(pathKey, new ResolvedSnapshotFile(relativePath, object));
        }
        return new ArrayList<>(uniquePaths.values());
    }

    /** changedFileCount 同时统计新增、内容修改和路径删除。 */
    private int calculateChangedFileCount(String parentSnapshotId, List<ResolvedSnapshotFile> currentFiles) {
        if (parentSnapshotId == null) {
            return currentFiles.size();
        }

        List<GameSnapshotFile> parentFiles = gameSnapshotFileMapper.selectList(
                new LambdaQueryWrapper<GameSnapshotFile>()
                        .eq(GameSnapshotFile::getSnapshotId, parentSnapshotId));
        Map<String, String> parentSignatures = new HashMap<>();
        for (GameSnapshotFile parentFile : parentFiles) {
            parentSignatures.put(
                    parentFile.getRelativePath().toLowerCase(Locale.ROOT),
                    signature(parentFile.getSha256(), parentFile.getSize()));
        }

        int changed = 0;
        Set<String> currentPaths = new HashSet<>();
        for (ResolvedSnapshotFile currentFile : currentFiles) {
            String pathKey = currentFile.getRelativePath().toLowerCase(Locale.ROOT);
            currentPaths.add(pathKey);
            String currentSignature = signature(
                    currentFile.getObject().getSha256(), currentFile.getObject().getSize());
            if (!currentSignature.equals(parentSignatures.get(pathKey))) {
                changed++;
            }
        }
        for (String parentPath : parentSignatures.keySet()) {
            if (!currentPaths.contains(parentPath)) {
                changed++;
            }
        }
        return changed;
    }

    private long calculateLogicalSize(List<ResolvedSnapshotFile> files) {
        long total = 0L;
        try {
            for (ResolvedSnapshotFile file : files) {
                total = Math.addExact(total, file.getObject().getSize());
            }
            return total;
        } catch (ArithmeticException e) {
            throw GameSaveException.badRequest("MANIFEST_TOO_LARGE", "快照逻辑大小超出支持范围");
        }
    }

    private GameLibrary requireOwnedGame(String gameId, String userId) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_GAME_ID", "gameId 不能为空");
        }
        GameLibrary game = gameLibraryMapper.selectOne(new LambdaQueryWrapper<GameLibrary>()
                .eq(GameLibrary::getGameId, gameId.trim())
                .eq(GameLibrary::getUserId, userId)
                .eq(GameLibrary::getStatus, 1)
                .last("LIMIT 1"));
        if (game == null) {
            throw GameSaveException.notFound("GAME_NOT_FOUND", "游戏不存在");
        }
        return game;
    }

    private GameSyncHead ensureAndGetHead(String userId, String gameId) {
        gameSyncHeadMapper.ensureHead(userId, gameId);
        GameSyncHead head = findHead(userId, gameId);
        if (head == null) {
            throw new IllegalStateException("同步 HEAD 初始化失败");
        }
        return head;
    }

    private GameSyncHead findHead(String userId, String gameId) {
        return gameSyncHeadMapper.selectOne(new LambdaQueryWrapper<GameSyncHead>()
                .eq(GameSyncHead::getUserId, userId)
                .eq(GameSyncHead::getGameId, gameId)
                .last("LIMIT 1"));
    }

    private void validateRequest(SnapshotCommitRequest request) {
        if (request == null) {
            throw GameSaveException.badRequest("INVALID_SNAPSHOT_REQUEST", "快照提交请求不能为空");
        }
        String triggerType = request.getTriggerType() == null
                ? ""
                : request.getTriggerType().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TRIGGER_TYPES.contains(triggerType)) {
            throw GameSaveException.badRequest("INVALID_TRIGGER_TYPE", "不支持的快照触发类型");
        }
        if (request.getDescription() != null && request.getDescription().trim().length() > 500) {
            throw GameSaveException.badRequest("DESCRIPTION_TOO_LONG", "快照描述长度不能超过 500");
        }
    }

    private String normalizeRelativePath(String path) {
        if (path == null) {
            throw GameSaveException.badRequest("INVALID_PATH", "快照文件路径不能为空");
        }
        String normalized = path.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.length() > 1024 || normalized.startsWith("/") || normalized.contains(":")) {
            throw GameSaveException.badRequest("INVALID_PATH", "非法快照相对路径: " + path);
        }
        String[] segments = normalized.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw GameSaveException.badRequest("INVALID_PATH", "非法快照相对路径: " + path);
            }
        }
        return normalized;
    }

    private String normalizeNullableId(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String normalizeDescription(String description) {
        return description == null || description.trim().isEmpty() ? null : description.trim();
    }

    private String signature(String sha256, long size) {
        return sha256 + ":" + size;
    }

    /** 已通过用户归属校验的 Manifest 文件。 */
    @Getter
    @AllArgsConstructor
    private static class ResolvedSnapshotFile {
        private final String relativePath;
        private final GameObject object;
    }
}
