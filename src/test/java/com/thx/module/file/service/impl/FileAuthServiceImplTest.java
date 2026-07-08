package com.thx.module.file.service.impl;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.CallerType;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.model.FileAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FileAuthServiceImpl 权限校验测试
 * 覆盖跨 App 隔离（一律 404）、PUBLIC/APP_INTERNAL/OWNER_ONLY 三种访问级别的允许与拒绝分支、
 * 未知访问级别的默认拒绝等场景
 */
class FileAuthServiceImplTest {

    private FileAuthServiceImpl fileAuthService;

    @BeforeEach
    void setUp() {
        fileAuthService = new FileAuthServiceImpl();
    }

    /** 构造测试用的调用方上下文，固定 CallerType 为 APPLICATION、Scope 为 READ */
    private FileCallerContext caller(String appId, String userId) {
        FileCallerContext ctx = new FileCallerContext();
        ctx.setAppId(appId);
        ctx.setUserId(userId);
        ctx.setCallerType(CallerType.APPLICATION);
        ctx.setScopes(Collections.singleton("READ"));
        return ctx;
    }

    /** 构造测试用的文件资产，只设置权限校验关心的字段 */
    private FileAsset asset(String appId, String accessLevel, String ownerId) {
        FileAsset asset = new FileAsset();
        asset.setAppId(appId);
        asset.setAccessLevel(accessLevel);
        asset.setOwnerId(ownerId);
        return asset;
    }

    @Test
    void crossAppReadThrowsNotFound() {
        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> fileAuthService.checkRead(caller("app-a", null), asset("app-b", "PUBLIC", null)));
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void crossAppDeleteThrowsNotFound() {
        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> fileAuthService.checkDelete(caller("app-a", "user-1"), asset("app-b", "OWNER_ONLY", "user-1")));
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void publicFileReadableBySameApp() {
        assertDoesNotThrow(() -> fileAuthService.checkRead(caller("app-a", null), asset("app-a", "PUBLIC", null)));
    }

    @Test
    void appInternalReadableBySameApp() {
        assertDoesNotThrow(() -> fileAuthService.checkRead(caller("app-a", null), asset("app-a", "APP_INTERNAL", null)));
    }

    @Test
    void ownerOnlyReadableByMatchingOwner() {
        assertDoesNotThrow(() -> fileAuthService.checkRead(caller("app-a", "user-1"), asset("app-a", "OWNER_ONLY", "user-1")));
    }

    @Test
    void ownerOnlyRejectsDifferentOwner() {
        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> fileAuthService.checkRead(caller("app-a", "user-2"), asset("app-a", "OWNER_ONLY", "user-1")));
        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void ownerOnlyRejectsNullCallerUserId() {
        assertThrows(FileSystemException.class,
                () -> fileAuthService.checkRead(caller("app-a", null), asset("app-a", "OWNER_ONLY", "user-1")));
    }

    @Test
    void unknownAccessLevelDefaultsDeny() {
        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> fileAuthService.checkRead(caller("app-a", null), asset("app-a", "SOMETHING_ELSE", null)));
        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void checkDeleteAppliesSameOwnerRule() {
        assertDoesNotThrow(() -> fileAuthService.checkDelete(caller("app-a", "user-1"), asset("app-a", "OWNER_ONLY", "user-1")));
    }
}
