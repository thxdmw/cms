package com.thx.module.gamesave.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** GameSave 后台任务与认证参数，测试环境可覆盖为更短阈值。 */
@Data
@Component
@ConfigurationProperties(prefix = "gamesave")
public class GameSaveProperties {

    private int tokenExpireDays = 90;
    private int lastSeenUpdateMinutes = 10;
    private int loginRateLimitMinutes = 5;
    private int loginUserIpFailures = 5;
    private int loginIpFailures = 20;
    private int registerIpAttempts = 10;
    private int objectCleanupBatchSize = 100;
    private int orphanObjectHours = 24;
    private int gameCleanupSnapshotBatchSize = 20;
    private int gameCleanupFileBatchSize = 1000;
}
