package com.thx.module.gamesave.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameSaveProtocolLimitsTest {

    @Test
    void protocolLimitsShouldMatchDesktopClientContract() {
        assertEquals(5000, GameSaveProtocolLimits.MAXIMUM_MANIFEST_FILES);
        assertEquals(200, GameSaveProtocolLimits.MAXIMUM_SNAPSHOT_LIST_LIMIT);
        assertEquals(1024, GameSaveProtocolLimits.RELATIVE_PATH_MAX_LENGTH);
        assertEquals(500, GameSaveProtocolLimits.DESCRIPTION_MAX_LENGTH);
    }
}
