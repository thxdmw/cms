package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户配额原子预占和安全释放测试。 */
@ExtendWith(MockitoExtension.class)
class GameQuotaServiceImplTest {

    @Mock
    private GameAccountMapper gameAccountMapper;

    private GameQuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GameQuotaServiceImpl(gameAccountMapper);
    }

    @Test
    void reserveShouldUseAtomicMapperUpdate() {
        when(gameAccountMapper.reserveQuota("user-1", 1024L)).thenReturn(1);

        service.reserve("user-1", 1024L);

        verify(gameAccountMapper).reserveQuota("user-1", 1024L);
    }

    @Test
    void reserveWithoutRemainingCapacityShouldReturnStableError() {
        when(gameAccountMapper.reserveQuota("user-1", 1024L)).thenReturn(0);

        GameSaveException exception = assertThrows(GameSaveException.class,
                () -> service.reserve("user-1", 1024L));

        assertEquals("QUOTA_EXCEEDED", exception.getCode());
    }

    @Test
    void releaseMustRejectAccountingUnderflow() {
        when(gameAccountMapper.releaseQuota("user-1", 1024L)).thenReturn(0);

        GameSaveException exception = assertThrows(GameSaveException.class,
                () -> service.release("user-1", 1024L));

        assertEquals("QUOTA_STATE_CHANGED", exception.getCode());
    }
}