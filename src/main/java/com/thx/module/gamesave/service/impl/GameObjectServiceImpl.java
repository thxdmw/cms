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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** GameSave 用户级内容去重实现。 */
@Service
@RequiredArgsConstructor
public class GameObjectServiceImpl implements GameObjectService {

    private static final String APP_ID = "game-save";
    private static final String SAVE_NAMESPACE = "save-object";
    private static final String ACTIVE = "ACTIVE";
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-f0-9]{64}$");
    private static final Set<String> FILE_SCOPES = new LinkedHashSet<>(
            Arrays.asList("UPLOAD", "READ", "DELETE", "LIST", "PRESIGN"));

    private final GameObjectMapper gameObjectMapper;
    private final FileSystemService fileSystemService;
    private final FileObjectLookupService fileObjectLookupService;

    @Override
    public List<ObjectDescriptor> findMissing(List<ObjectDescriptor> objects, GameCallerContext caller) {
        List<ObjectDescriptor> missing = new ArrayList<>();
        if (objects == null) {
            return missing;
        }
        for (ObjectDescriptor descriptor : objects) {
            validateDescriptor(descriptor.getSha256(), descriptor.getSize());
            if (findObject(caller.getUserId(), descriptor.getSha256(), descriptor.getSize()) == null) {
                missing.add(descriptor);
            }
        }
        return missing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        GameObject existing = findObject(caller.getUserId(), normalizedHash, expectedSize);
        if (existing != null) {
            return existing;
        }

        FileCallerContext fileCaller = fileCaller(caller);
        FileInfoResult fileInfo = fileObjectLookupService.findActiveByHash(
                SAVE_NAMESPACE, normalizedHash, expectedSize, fileCaller);
        boolean uploadedNow = false;
        if (fileInfo == null) {
            FileUploadResult uploadResult = fileSystemService.upload(
                    file, SAVE_NAMESPACE, caller.getUserId(), fileCaller);
            fileInfo = fileSystemService.get(uploadResult.getFileId(), fileCaller);
            uploadedNow = true;
        }

        if (!normalizedHash.equals(normalizeHash(fileInfo.getSha256())) || expectedSize != fileInfo.getSize()) {
            if (uploadedNow) {
                fileSystemService.delete(fileInfo.getFileId(), fileCaller);
            }
            throw GameSaveException.badRequest("CHECKSUM_MISMATCH", "服务端文件校验结果与客户端对象描述不一致");
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
            return object;
        } catch (DuplicateKeyException duplicate) {
            GameObject winner = findObject(caller.getUserId(), normalizedHash, expectedSize);
            if (uploadedNow) {
                fileSystemService.delete(fileInfo.getFileId(), fileCaller);
            }
            if (winner != null) {
                return winner;
            }
            throw duplicate;
        }
    }

    private GameObject findObject(String userId, String sha256, long size) {
        return gameObjectMapper.selectOne(new LambdaQueryWrapper<GameObject>()
                .eq(GameObject::getUserId, userId)
                .eq(GameObject::getSha256, normalizeHash(sha256))
                .eq(GameObject::getSize, size)
                .eq(GameObject::getStatus, ACTIVE)
                .last("LIMIT 1"));
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
