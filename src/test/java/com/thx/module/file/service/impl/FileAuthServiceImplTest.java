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

class FileAuthServiceImplTest {

    private FileAuthServiceImpl fileAuthService;

    @BeforeEach
    void setUp() {
        fileAuthService = new FileAuthServiceImpl();
    }

    private FileCallerContext caller(String appId, String userId) {
        FileCallerContext ctx = new FileCallerContext();
        ctx.setAppId(appId);
        ctx.setUserId(userId);
        ctx.setCallerType(CallerType.APPLICATION);
        ctx.setScopes(Collections.singleton("READ"));
        return ctx;
    }

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
