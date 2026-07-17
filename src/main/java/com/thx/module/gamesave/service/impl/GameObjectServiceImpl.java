package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.util.UUIDUtil;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.context.FileCallerContextFactory;
import com.thx.module.file.service.FileObjectLookupService;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.file.vo.FileInfoResult;
import com.thx.module.file.vo.FileUploadResult;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.service.GameObjectService;
import com.thx.module.gamesave.service.GameQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** GameSave 用户级内容寻址与去重实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameObjectServiceImpl implements GameObjectService {
    @Override
    public String getDownloadUrl(String objectId, GameCallerContext caller) {
        if (objectId == null || objectId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_OBJECT_ID", "对象 ID 不能为空");
        }
        GameObject object = gameObjectMapper.selectOne(new LambdaQueryWrapper<GameObject>()
                .eq(GameObject::getObjectId, objectId.trim())
                .eq(GameObject::getUserId, caller.getUserId())
                .eq(GameObject::getStatus, ACTIVE)
                .last("LIMIT 1"));
        if (object == null) {
            throw GameSaveException.notFound("OBJECT_NOT_FOUND", "内容对象不存在或无权访问");
        }
        return fileSystemService.getDownloadUrl(object.getFileId(), fileCaller(caller));
    }

    private static final String APP_ID = "game-save";
    private static final String SAVE_NAMESPACE = "save-object";
    private static final String ACTIVE = "ACTIVE";
    private static final String DELETING = "DELETING";
    private static final String DELETED = "DELETED";
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-f0-9]{64}$");
    private static final Set<String> FILE_SCOPES = new LinkedHashSet<>(
            Arrays.asList("UPLOAD", "READ", "DELETE", "LIST", "PRESIGN"));
    // 单次请求允许校验/解析的内容对象上限，避免无界列表触发超大批量查询或拖垮数据库。
    private static final int MAX_CHECK_OBJECTS = 5000;

    private final GameObjectMapper gameObjectMapper;
    private final FileSystemService fileSystemService;
    private final FileObjectLookupService fileObjectLookupService;
    private final GameQuotaService gameQuotaService;

    @Override
    public List<ObjectDescriptor> findMissing(List<ObjectDescriptor> objects, GameCallerContext caller) {
        List<ObjectDescriptor> missing = new ArrayList<>();
        if (objects == null) {
            return missing;
        }
        if (objects.size() > MAX_CHECK_OBJECTS) {
            throw GameSaveException.badRequest(
                    "TOO_MANY_OBJECTS", "单次校验的内容对象数量不能超过 " + MAX_CHECK_OBJECTS);
        }

        // 一个 Manifest 中多个路径可能引用相同内容，检查接口只返回唯一内容对象。
        Map<String, ObjectDescriptor> uniqueObjects = new LinkedHashMap<>();
        for (ObjectDescriptor descriptor : objects) {
            if (descriptor == null) {
                throw GameSaveException.badRequest("INVALID_OBJECT", "对象描述不能为空");
            }
            String hash = normalizeHash(descriptor.getSha256());
            validateDescriptor(hash, descriptor.getSize());
            uniqueObjects.put(hash + ":" + descriptor.getSize(), new ObjectDescriptor(hash, descriptor.getSize()));
        }
        if (uniqueObjects.isEmpty()) {
            return missing;
        }

        // 一次批量查询替代逐个对象的判重往返。
        Set<String> owned = new LinkedHashSet<>();
        for (GameObject object : gameObjectMapper.selectActiveByDescriptors(
                caller.getUserId(), new ArrayList<>(uniqueObjects.values()))) {
            if (gameObjectMapper.touchActiveObject(object.getId(), caller.getUserId()) == 1) {
                owned.add(normalizeHash(object.getSha256()) + ":" + object.getSize());
            }
        }
        for (ObjectDescriptor descriptor : uniqueObjects.values()) {
            if (!owned.contains(descriptor.getSha256() + ":" + descriptor.getSize())) {
                missing.add(descriptor);
            }
        }
        return missing;
    }

    @Override
    public GameObject put(MultipartFile file,
                          String expectedSha256,
                          long expectedSize,
                          GameCallerContext caller) {
        String normalizedHash = normalizeHash(expectedSha256);
        validateDescriptor(normalizedHash, expectedSize);
        if (file == null || file.isEmpty()) {
            throw GameSaveException.badRequest("EMPTY_FILE", "文件不能为空");
        }
        if (file.getSize() != expectedSize) {
            throw GameSaveException.badRequest("SIZE_MISMATCH", "文件大小与对象描述不一致");
        }

        GameObject existing = findAnyObject(caller.getUserId(), normalizedHash, expectedSize);
        if (existing != null && ACTIVE.equals(existing.getStatus())) {
            if (gameObjectMapper.touchActiveObject(existing.getId(), caller.getUserId()) != 1) {
                throw GameSaveException.conflict(
                        "OBJECT_STATE_CHANGED", "相同内容对象的状态已变化，请重新检查缺失对象");
            }
            return existing;
        }
        if (existing != null && DELETING.equals(existing.getStatus())) {
            throw GameSaveException.conflict("OBJECT_CLEANUP_IN_PROGRESS", "相同内容正在清理，请稍后重试");
        }

        // 首次建立用户内容对象前原子预占配额；并发重复上传的失败方会在 finally 中释放。
        gameQuotaService.reserve(caller.getUserId(), expectedSize);
        boolean reservationRetained = false;
        try {
            FileCallerContext fileCaller = fileCaller(caller);
            FileInfoResult fileInfo = fileObjectLookupService.findActiveByHash(
                    SAVE_NAMESPACE, normalizedHash, expectedSize, fileCaller);
            boolean uploadedNow = false;
            if (fileInfo == null) {
                // FileSystemService.upload 自己负责文件元数据事务和对象存储写失败补偿。
                // 这里不使用外层长事务，避免数据库回滚后 MinIO 对象无法同步回滚。
                FileUploadResult uploadResult = fileSystemService.upload(
                        file, SAVE_NAMESPACE, caller.getUserId(), fileCaller);
                fileInfo = fileSystemService.get(uploadResult.getFileId(), fileCaller);
                uploadedNow = true;
            }

            if (!normalizedHash.equals(normalizeHash(fileInfo.getSha256())) || expectedSize != fileInfo.getSize()) {
                if (uploadedNow) {
                    cleanupUploadedFile(fileInfo.getFileId(), fileCaller);
                }
                throw GameSaveException.badRequest("CHECKSUM_MISMATCH", "服务端文件校验结果与客户端对象描述不一致");
            }

            if (existing != null && DELETED.equals(existing.getStatus())) {
                int reactivated = gameObjectMapper.reactivateDeleted(
                        existing.getId(), caller.getUserId(), fileInfo.getFileId());
                if (reactivated == 1) {
                    reservationRetained = true;
                    return findActiveObject(caller.getUserId(), normalizedHash, expectedSize);
                }
                if (uploadedNow) {
                    cleanupUploadedFile(fileInfo.getFileId(), fileCaller);
                }
                GameObject winner = findAnyObject(caller.getUserId(), normalizedHash, expectedSize);
                if (winner != null && ACTIVE.equals(winner.getStatus())) {
                    return winner;
                }
                throw GameSaveException.conflict("OBJECT_REACTIVATION_CONFLICT", "相同内容重新激活发生并发冲突，请重试");
            }

            GameObject object = new GameObject()
                    .setObjectId(UUIDUtil.uuid())
                    .setUserId(caller.getUserId())
                    .setSha256(normalizedHash)
                    .setSize(expectedSize)
                    .setFileId(fileInfo.getFileId())
                    .setReferenceCount(0L)
                    .setStatus(ACTIVE);
            try {
                gameObjectMapper.insert(object);
                reservationRetained = true;
                return object;
            } catch (DuplicateKeyException duplicate) {
                GameObject winner = findAnyObject(caller.getUserId(), normalizedHash, expectedSize);
                if (uploadedNow) {
                    cleanupUploadedFile(fileInfo.getFileId(), fileCaller);
                }
                if (winner != null && ACTIVE.equals(winner.getStatus())) {
                    return winner;
                }
                throw duplicate;
            } catch (RuntimeException exception) {
                if (uploadedNow) {
                    cleanupUploadedFile(fileInfo.getFileId(), fileCaller);
                }
                throw exception;
            }
        } finally {
            if (!reservationRetained) {
                releaseQuotaReservation(caller.getUserId(), expectedSize);
            }
        }
    }

    @Override
    public void releaseSnapshotReference(String objectId, GameCallerContext caller) {
        if (objectId == null || objectId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_OBJECT_ID", "对象 ID 不能为空");
        }
        String normalizedObjectId = objectId.trim();
        int updated = gameObjectMapper.decrementReference(normalizedObjectId, caller.getUserId());
        if (updated != 1) {
            throw GameSaveException.conflict("OBJECT_REFERENCE_CHANGED", "对象引用状态已变化，请重新加载快照时间线");
        }

        GameObject object = gameObjectMapper.selectOne(new LambdaQueryWrapper<GameObject>()
                .eq(GameObject::getObjectId, normalizedObjectId)
                .eq(GameObject::getUserId, caller.getUserId())
                .last("LIMIT 1"));
        if (object == null) {
            throw GameSaveException.notFound("OBJECT_NOT_FOUND", "内容对象不存在");
        }
        if (object.getReferenceCount() != null && object.getReferenceCount() == 0L) {
            int deleting = gameObjectMapper.markDeletingIfUnreferenced(normalizedObjectId, caller.getUserId());
            if (deleting != 1) {
                throw GameSaveException.conflict("OBJECT_STATE_CHANGED", "内容对象状态已变化，请重新加载快照时间线");
            }
        }
    }
    @Override
    public Map<String, GameObject> requireOwnedObjects(List<ObjectDescriptor> descriptors, GameCallerContext caller) {
        if (descriptors == null || descriptors.isEmpty()) {
            return new LinkedHashMap<>();
        }
        if (descriptors.size() > MAX_CHECK_OBJECTS) {
            throw GameSaveException.badRequest(
                    "TOO_MANY_OBJECTS", "单次提交引用的内容对象数量不能超过 " + MAX_CHECK_OBJECTS);
        }

        Map<String, ObjectDescriptor> uniqueDescriptors = new LinkedHashMap<>();
        for (ObjectDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                throw GameSaveException.badRequest("INVALID_OBJECT", "对象描述不能为空");
            }
            String hash = normalizeHash(descriptor.getSha256());
            validateDescriptor(hash, descriptor.getSize());
            uniqueDescriptors.put(hash + ":" + descriptor.getSize(), new ObjectDescriptor(hash, descriptor.getSize()));
        }

        // 一次批量查询替代逐个对象的归属校验往返；缺失任意一个都拒绝整个 Manifest。
        Map<String, GameObject> owned = new LinkedHashMap<>();
        for (GameObject object : gameObjectMapper.selectActiveByDescriptors(
                caller.getUserId(), new ArrayList<>(uniqueDescriptors.values()))) {
            owned.put(normalizeHash(object.getSha256()) + ":" + object.getSize(), object);
        }
        for (String key : uniqueDescriptors.keySet()) {
            if (!owned.containsKey(key)) {
                throw GameSaveException.notFound("OBJECT_MISSING", "快照引用的内容对象尚未上传");
            }
        }
        return owned;
    }

    /**
     * 清理本次请求刚上传、但未成功建立 game_object 关系的文件资产。
     * 清理异常只记录日志，不能覆盖真正的业务/数据库异常；文件模块自己的清理任务负责后续重试。
     */
    /** 配额补偿失败只记录错误，不能覆盖上传链路的原始异常；后续由配额校准任务修复。 */
    private void releaseQuotaReservation(String userId, long bytes) {
        try {
            gameQuotaService.release(userId, bytes);
        } catch (RuntimeException quotaException) {
            log.error("GameSave 配额预占释放失败，等待配额校准任务修复: userId={}, bytes={}",
                    userId, bytes, quotaException);
        }
    }
    private void cleanupUploadedFile(String fileId, FileCallerContext caller) {
        try {
            fileSystemService.delete(fileId, caller);
        } catch (RuntimeException cleanupException) {
            log.error("GameSave 内容对象补偿删除失败，等待文件清理机制后续处理: fileId={}", fileId, cleanupException);
        }
    }

    private GameObject findActiveObject(String userId, String sha256, long size) {
        return gameObjectMapper.selectOne(new LambdaQueryWrapper<GameObject>()
                .eq(GameObject::getUserId, userId)
                .eq(GameObject::getSha256, normalizeHash(sha256))
                .eq(GameObject::getSize, size)
                .eq(GameObject::getStatus, ACTIVE)
                .last("LIMIT 1"));
    }

    private GameObject findAnyObject(String userId, String sha256, long size) {
        return gameObjectMapper.selectAnyByDescriptor(userId, normalizeHash(sha256), size);
    }

    private FileCallerContext fileCaller(GameCallerContext caller) {
        return FileCallerContextFactory.forInternalApp(
                APP_ID, caller.getUserId(), FILE_SCOPES, caller.getIp());
    }

    private void validateDescriptor(String sha256, long size) {
        String normalizedHash = normalizeHash(sha256);
        if (!SHA256_PATTERN.matcher(normalizedHash).matches()) {
            throw GameSaveException.badRequest("INVALID_SHA256", "sha256 必须是 64 位十六进制字符串");
        }
        if (size < 0) {
            throw GameSaveException.badRequest("INVALID_SIZE", "文件大小不能小于 0");
        }
    }

    private String normalizeHash(String sha256) {
        return sha256 == null ? "" : sha256.trim().toLowerCase(Locale.ROOT);
    }
}
