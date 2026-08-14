package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 设备撤销的用户边界和当前设备保护测试。 */
@ExtendWith(MockitoExtension.class)
class GameDeviceServiceImplTest {

    @Mock
    private GameDeviceMapper gameDeviceMapper;

    private GameDeviceServiceImpl service;
    private GameCallerContext caller;

    @BeforeEach
    void setUp() {
        service = new GameDeviceServiceImpl(gameDeviceMapper);
        caller = new GameCallerContext();
        caller.setUserId("user-1");
        caller.setDeviceId("device-current");
    }

    @Test
    void revokeOtherOwnedDeviceShouldUseUserScopedAtomicUpdate() {
        when(gameDeviceMapper.revokeActiveDevice("device-other", "user-1")).thenReturn(1);

        service.revoke(" device-other ", caller);

        verify(gameDeviceMapper).revokeActiveDevice("device-other", "user-1");
    }

    @Test
    void revokeCurrentDeviceShouldBeRejectedWithoutDatabaseWrite() {
        GameSaveException exception = assertThrows(GameSaveException.class,
                () -> service.revoke("device-current", caller));

        assertEquals("CANNOT_REVOKE_CURRENT_DEVICE", exception.getCode());
        verify(gameDeviceMapper, never()).revokeActiveDevice("device-current", "user-1");
    }

    @Test
    void revokeUnknownOrForeignDeviceShouldReturnNotFound() {
        when(gameDeviceMapper.revokeActiveDevice("device-foreign", "user-1")).thenReturn(0);

        GameSaveException exception = assertThrows(GameSaveException.class,
                () -> service.revoke("device-foreign", caller));

        assertEquals("DEVICE_NOT_FOUND", exception.getCode());
    }
}