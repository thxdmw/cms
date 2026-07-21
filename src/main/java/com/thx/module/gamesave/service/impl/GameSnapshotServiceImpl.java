package com.thx.module.gamesave.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.util.UUIDUtil;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.config.GameSaveProtocolLimits;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.dto.SnapshotCommitRequest;
import com.thx.module.gamesave.dto.SnapshotCommitResult;
import com.thx.module.gamesave.dto.SnapshotFileDescriptor;
import com.thx.module.gamesave.dto.SnapshotRootDescriptor;
import com.thx.module.gamesave.dto.SnapshotRootResult;
import com.thx.module.gamesave.dto.SyncHeadResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSnapshotRootMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.model.GameSnapshotFile;
import com.thx.module.gamesave.model.GameSnapshotRoot;
import com.thx.module.gamesave.model.GameSyncHead;
import com.thx.module.gamesave.service.GameObjectService;
import com.thx.module.gamesave.service.GameSnapshotService;
import com.thx.module.gamesave.util.GameTokenUtil;
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
import java.util.Collections;
import java.util.regex.Pattern;

/** GameSave 不可变快照事务实现。 */
@Service
@RequiredArgsConstructor
public class GameSnapshotServiceImpl implements GameSnapshotService {
    @Override
    public List<com.thx.module.gamesave.dto.SnapshotSummaryResult> listSnapshots(
            String gameId, int limit, GameCallerContext caller) {
        requireOwnedGame(gameId, caller.getUserId());
        int safeLimit = Math.max(1, Math.min(limit, GameSaveProtocolLimits.MAXIMUM_SNAPSHOT_LIST_LIMIT));
        List<GameSnapshot> snapshots = gameSnapshotMapper.selectList(
                new LambdaQueryWrapper<GameSnapshot>()
                        .eq(GameSnapshot::getGameId, gameId.trim())
                        .eq(GameSnapshot::getUserId, caller.getUserId())
                        .eq(GameSnapshot::getStatus, ACTIVE)
                        .orderByDesc(GameSnapshot::getCreateTime)
                        .last("LIMIT " + safeLimit));
        List<com.thx.module.gamesave.dto.SnapshotSummaryResult> results =
                new ArrayList<>(snapshots.size());
        Map<String, List<SnapshotRootResult>> rootsBySnapshot = loadRoots(
                snapshots.stream().map(GameSnapshot::getSnapshotId).collect(java.util.stream.Collectors.toList()));
        for (GameSnapshot snapshot : snapshots) {
            results.add(com.thx.module.gamesave.dto.SnapshotSummaryResult.from(
                    snapshot,
                    rootsBySnapshot.getOrDefault(snapshot.getSnapshotId(), Collections.emptyList())));
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
        List<SnapshotRootResult> roots = loadRoots(Collections.singletonList(snapshot.getSnapshotId()))
                .getOrDefault(snapshot.getSnapshotId(), Collections.emptyList());
        return new com.thx.module.gamesave.dto.SnapshotManifestResult(
                snapshot.getSnapshotId(),
                snapshot.getGameId(),
                snapshot.getDeviceId(),
                snapshot.getParentSnapshotId(),
                snapshot.getTriggerType(),
                snapshot.getDescription(),
                snapshot.getCreateTime(),
                roots,
                files);
    }

    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> ALLOWED_TRIGGER_TYPES = new HashSet<>(
            Arrays.asList("MANUAL", "GAME_EXIT", "BEFORE_RESTORE", "IMPORT"));
    // 单个快照允许提交的文件数量上限，避免无界 Manifest 触发超大批量查询或拖垮数据库。
    private static final int MAX_MANIFEST_FILES = GameSaveProtocolLimits.MAXIMUM_MANIFEST_FILES;
    private static final Pattern ROOT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Set<String> ALLOWED_ROOT_TYPES = new HashSet<>(Arrays.asList("FILE", "REGISTRY"));

    private final GameLibraryMapper gameLibraryMapper;
    private final GameSnapshotMapper gameSnapshotMapper;
    private final GameSnapshotFileMapper gameSnapshotFileMapper;
    private final GameSnapshotRootMapper gameSnapshotRootMapper;
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
    public void deleteSnapshot(String gameId, String snapshotId, GameCallerContext caller) {
        requireOwnedGame(gameId, caller.getUserId());
        if (snapshotId == null || snapshotId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_SNAPSHOT_ID", "快照 ID 不能为空");
        }
        GameSyncHead head = ensureAndGetHead(caller.getUserId(), gameId);
        if (snapshotId.trim().equals(head.getHeadSnapshotId())) {
            throw GameSaveException.badRequest("CANNOT_DELETE_HEAD", "不能删除当前同步 HEAD；请先创建或恢复其他版本");
        }
        GameSnapshot snapshot = gameSnapshotMapper.selectOne(new LambdaQueryWrapper<GameSnapshot>()
                .eq(GameSnapshot::getSnapshotId, snapshotId.trim())
                .eq(GameSnapshot::getUserId, caller.getUserId())
                .eq(GameSnapshot::getGameId, gameId)
                .eq(GameSnapshot::getStatus, ACTIVE)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw GameSaveException.notFound("SNAPSHOT_NOT_FOUND", "快照不存在或已经删除");
        }
        List<GameSnapshotFile> files = gameSnapshotFileMapper.selectList(new LambdaQueryWrapper<GameSnapshotFile>()
                .eq(GameSnapshotFile::getSnapshotId, snapshot.getSnapshotId()));
        for (GameSnapshotFile file : files) {
            gameObjectService.releaseSnapshotReference(file.getObjectId(), caller);
        }
        int updated = gameSnapshotMapper.markDeleted(
                snapshot.getSnapshotId(), caller.getUserId(), gameId.trim());
        if (updated != 1) {
            throw GameSaveException.conflict("SNAPSHOT_STATE_CHANGED", "快照状态已变化，请重新加载时间线");
        }
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
        List<SnapshotRootDescriptor> roots = validateRoots(request.getRoots(), resolvedFiles);
        long logicalSize = calculateLogicalSize(resolvedFiles);
        int changedFileCount = calculateChangedFileCount(currentHead.getHeadSnapshotId(), resolvedFiles);

        // 已有 HEAD 且 Manifest 完全一致时直接返回当前版本，避免重复同步制造零变化快照、
        // 重复 snapshot_file 元数据以及无意义的对象引用计数增长。
        if (currentHead.getHeadSnapshotId() != null && changedFileCount == 0
                && rootMetadataMatches(currentHead.getHeadSnapshotId(), roots)) {
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

        for (SnapshotRootDescriptor root : roots) {
            gameSnapshotRootMapper.insert(new GameSnapshotRoot()
                    .setSnapshotId(snapshotId)
                    .setRootId(root.getRootId().trim())
                    .setRootType(root.getRootType().trim().toUpperCase(Locale.ROOT))
                    .setPathTemplate(normalizeNullableText(root.getPathTemplate()))
                    .setSource(normalizeSource(root.getSource()))
                    .setConfidence(root.getConfidence() == null ? 0 : root.getConfidence())
                    .setIncludePatternsJson(JSON.toJSONString(normalizePatterns(root.getIncludePatterns())))
                    .setExcludePatternsJson(JSON.toJSONString(normalizePatterns(root.getExcludePatterns()))));
        }

        for (ResolvedSnapshotFile resolved : resolvedFiles) {
            GameSnapshotFile snapshotFile = new GameSnapshotFile()
                    .setSnapshotId(snapshotId)
                    .setRelativePath(resolved.getRelativePath())
                    .setRelativePathHash(GameTokenUtil.sha256Hex(resolved.getRelativePath()))
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

    /**
     * 将客户端 Manifest 解析为当前用户实际拥有的内容对象，并完成路径规范化与判重。
     * 第一遍只做路径校验、不访问数据库；第二遍对全部文件引用的内容对象发起一次批量归属校验，
     * 避免文件数量线性增长时逐文件触发数据库往返。
     */
    private List<ResolvedSnapshotFile> resolveManifest(List<SnapshotFileDescriptor> files,
                                                       GameCallerContext caller) {
        if (files == null) {
            throw GameSaveException.badRequest("INVALID_MANIFEST", "快照文件清单不能为空");
        }

        Map<String, SnapshotFileDescriptor> uniqueFiles = new LinkedHashMap<>();
        Map<String, String> pathsByKey = new LinkedHashMap<>();
        List<ObjectDescriptor> descriptors = new ArrayList<>(files.size());
        for (SnapshotFileDescriptor descriptor : files) {
            if (descriptor == null) {
                throw GameSaveException.badRequest("INVALID_MANIFEST_FILE", "快照文件描述不能为空");
            }
            String relativePath = normalizeRelativePath(descriptor.getPath());
            String pathKey = relativePath.toLowerCase(Locale.ROOT);
            if (uniqueFiles.containsKey(pathKey)) {
                throw GameSaveException.badRequest("DUPLICATE_PATH", "快照中存在重复路径: " + relativePath);
            }
            uniqueFiles.put(pathKey, descriptor);
            pathsByKey.put(pathKey, relativePath);
            descriptors.add(new ObjectDescriptor(descriptor.getSha256(), descriptor.getSize()));
        }

        Map<String, GameObject> ownedObjects = gameObjectService.requireOwnedObjects(descriptors, caller);
        List<ResolvedSnapshotFile> resolved = new ArrayList<>(uniqueFiles.size());
        for (Map.Entry<String, SnapshotFileDescriptor> entry : uniqueFiles.entrySet()) {
            SnapshotFileDescriptor descriptor = entry.getValue();
            String objectKey = normalizeHash(descriptor.getSha256()) + ":" + descriptor.getSize();
            resolved.add(new ResolvedSnapshotFile(pathsByKey.get(entry.getKey()), ownedObjects.get(objectKey)));
        }
        return resolved;
    }

    private String normalizeHash(String sha256) {
        return sha256 == null ? "" : sha256.trim().toLowerCase(Locale.ROOT);
    }

    private List<SnapshotRootDescriptor> validateRoots(List<SnapshotRootDescriptor> roots,
                                                       List<ResolvedSnapshotFile> files) {
        if (roots == null || roots.isEmpty()) {
            return Collections.emptyList();
        }
        if (roots.size() > GameSaveProtocolLimits.MAXIMUM_SNAPSHOT_ROOTS) {
            throw GameSaveException.badRequest("TOO_MANY_ROOTS", "单个快照的存档根目录数量不能超过 "
                    + GameSaveProtocolLimits.MAXIMUM_SNAPSHOT_ROOTS);
        }
        Set<String> rootIds = new HashSet<>();
        for (SnapshotRootDescriptor root : roots) {
            if (root == null || root.getRootId() == null
                    || !ROOT_ID_PATTERN.matcher(root.getRootId().trim()).matches()) {
                throw GameSaveException.badRequest("INVALID_ROOT_ID", "存档根目录标识不合法");
            }
            String rootId = root.getRootId().trim();
            if (!rootIds.add(rootId.toLowerCase(Locale.ROOT))) {
                throw GameSaveException.badRequest("DUPLICATE_ROOT_ID", "快照中存在重复存档根目录: " + rootId);
            }
            String rootType = root.getRootType() == null ? "" : root.getRootType().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_ROOT_TYPES.contains(rootType)) {
                throw GameSaveException.badRequest("INVALID_ROOT_TYPE", "存档根目录类型不受支持: " + rootType);
            }
            String pathTemplate = normalizeNullableText(root.getPathTemplate());
            if (pathTemplate != null && pathTemplate.length() > GameSaveProtocolLimits.PATH_TEMPLATE_MAX_LENGTH) {
                throw GameSaveException.badRequest("PATH_TEMPLATE_TOO_LONG", "存档路径模板过长");
            }
            if (pathTemplate != null && pathTemplate.chars().anyMatch(Character::isISOControl)) {
                throw GameSaveException.badRequest("INVALID_PATH_TEMPLATE", "存档路径模板包含控制字符");
            }
            int confidence = root.getConfidence() == null ? 0 : root.getConfidence();
            if (confidence < 0 || confidence > 100) {
                throw GameSaveException.badRequest("INVALID_ROOT_CONFIDENCE", "存档路径置信度必须在 0 到 100 之间");
            }
            normalizePatterns(root.getIncludePatterns());
            normalizePatterns(root.getExcludePatterns());
        }
        for (ResolvedSnapshotFile file : files) {
            int separator = file.getRelativePath().indexOf('/');
            String rootId = separator <= 0 ? "" : file.getRelativePath().substring(0, separator).toLowerCase(Locale.ROOT);
            if (!rootIds.contains(rootId)) {
                throw GameSaveException.badRequest("UNKNOWN_ROOT_ID", "快照文件引用了未声明的存档根目录: "
                        + file.getRelativePath());
            }
        }
        return roots;
    }

    private List<String> normalizePatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }
        if (patterns.size() > GameSaveProtocolLimits.MAXIMUM_PATTERNS_PER_ROOT) {
            throw GameSaveException.badRequest("TOO_MANY_ROOT_PATTERNS", "单个存档根目录的包含或排除规则过多");
        }
        List<String> normalized = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            if (pattern == null || pattern.trim().isEmpty()
                    || pattern.trim().length() > GameSaveProtocolLimits.PATTERN_MAX_LENGTH
                    || pattern.chars().anyMatch(Character::isISOControl)) {
                throw GameSaveException.badRequest("INVALID_ROOT_PATTERN", "存档根目录包含非法扫描规则");
            }
            normalized.add(pattern.trim());
        }
        return normalized;
    }

    private String normalizeSource(String source) {
        String normalized = source == null || source.trim().isEmpty()
                ? "UNKNOWN" : source.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 32) {
            throw GameSaveException.badRequest("INVALID_ROOT_SOURCE", "存档路径来源过长");
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Map<String, List<SnapshotRootResult>> loadRoots(List<String> snapshotIds) {
        if (snapshotIds == null || snapshotIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GameSnapshotRoot> rows = gameSnapshotRootMapper.selectList(
                new LambdaQueryWrapper<GameSnapshotRoot>()
                        .in(GameSnapshotRoot::getSnapshotId, snapshotIds)
                        .orderByAsc(GameSnapshotRoot::getId));
        Map<String, List<SnapshotRootResult>> result = new HashMap<>();
        for (GameSnapshotRoot row : rows) {
            result.computeIfAbsent(row.getSnapshotId(), ignored -> new ArrayList<>()).add(
                    SnapshotRootResult.from(
                            row,
                            parsePatterns(row.getIncludePatternsJson()),
                            parsePatterns(row.getExcludePatternsJson())));
        }
        return result;
    }

    /** 旧客户端不提交 roots 时维持原有幂等语义；新客户端提交的新路径元数据会生成一个新快照。 */
    private boolean rootMetadataMatches(String snapshotId, List<SnapshotRootDescriptor> requestedRoots) {
        if (requestedRoots == null || requestedRoots.isEmpty()) {
            return true;
        }
        List<SnapshotRootResult> existingRoots = loadRoots(Collections.singletonList(snapshotId))
                .getOrDefault(snapshotId, Collections.emptyList());
        if (existingRoots.size() != requestedRoots.size()) {
            return false;
        }
        Map<String, SnapshotRootResult> existingById = new HashMap<>();
        for (SnapshotRootResult root : existingRoots) {
            existingById.put(root.getRootId().toLowerCase(Locale.ROOT), root);
        }
        for (SnapshotRootDescriptor requested : requestedRoots) {
            SnapshotRootResult existing = existingById.get(requested.getRootId().trim().toLowerCase(Locale.ROOT));
            if (existing == null
                    || !Objects.equals(existing.getRootType(), requested.getRootType().trim().toUpperCase(Locale.ROOT))
                    || !Objects.equals(existing.getPathTemplate(), normalizeNullableText(requested.getPathTemplate()))
                    || !Objects.equals(existing.getSource(), normalizeSource(requested.getSource()))
                    || existing.getConfidence() != (requested.getConfidence() == null ? 0 : requested.getConfidence())
                    || !Objects.equals(existing.getIncludePatterns(), normalizePatterns(requested.getIncludePatterns()))
                    || !Objects.equals(existing.getExcludePatterns(), normalizePatterns(requested.getExcludePatterns()))) {
                return false;
            }
        }
        return true;
    }

    private List<String> parsePatterns(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> patterns = JSON.parseArray(json, String.class);
        return patterns == null ? Collections.emptyList() : patterns;
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
        if (request.getDescription() != null
                && request.getDescription().trim().length() > GameSaveProtocolLimits.DESCRIPTION_MAX_LENGTH) {
            throw GameSaveException.badRequest("DESCRIPTION_TOO_LONG", "快照描述长度不能超过 500");
        }
        if (request.getFiles() != null && request.getFiles().size() > MAX_MANIFEST_FILES) {
            throw GameSaveException.badRequest(
                    "TOO_MANY_FILES", "单个快照的文件数量不能超过 " + MAX_MANIFEST_FILES);
        }
    }

    private String normalizeRelativePath(String path) {
        if (path == null) {
            throw GameSaveException.badRequest("INVALID_PATH", "快照文件路径不能为空");
        }
        String normalized = path.trim().replace('\\', '/');
        if (normalized.isEmpty()
                || normalized.length() > GameSaveProtocolLimits.RELATIVE_PATH_MAX_LENGTH
                || normalized.startsWith("/")
                || normalized.contains(":")) {
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
