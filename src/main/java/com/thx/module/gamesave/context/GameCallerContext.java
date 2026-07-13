package com.thx.module.gamesave.context;

import lombok.Data;

/**
 * GameSave 业务调用方上下文。
 * 只保存当前已认证用户、设备和来源 IP，用于把用户身份安全桥接到文件系统内部调用。
 */
@Data
public class GameCallerContext {
    private String userId;
    private String deviceId;
    private String ip;
}
