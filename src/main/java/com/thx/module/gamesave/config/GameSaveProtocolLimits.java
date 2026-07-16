package com.thx.module.gamesave.config;

/** 与桌面客户端共同遵守的固定协议边界。 */
public final class GameSaveProtocolLimits {

    public static final int MAXIMUM_MANIFEST_FILES = 5000;
    public static final int MAXIMUM_SNAPSHOT_LIST_LIMIT = 200;
    public static final int RELATIVE_PATH_MAX_LENGTH = 1024;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private GameSaveProtocolLimits() {
    }
}
