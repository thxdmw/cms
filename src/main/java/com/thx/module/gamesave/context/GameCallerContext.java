package com.thx.module.gamesave.context;

import lombok.Data;

/**
 * GameSave business caller context.
 * It bridges game user identity into the file module internal call.
 */
@Data
public class GameCallerContext {
    private String userId;
    private String deviceId;
    private String ip;
}
