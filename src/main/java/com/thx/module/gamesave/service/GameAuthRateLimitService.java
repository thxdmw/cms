package com.thx.module.gamesave.service;

/** GameSave 注册与登录限流边界，避免 Controller 直接操作 Redis。 */
public interface GameAuthRateLimitService {

    void assertLoginAllowed(String username, String ip);

    void recordLoginFailure(String username, String ip);

    void recordLoginSuccess(String username, String ip);

    void assertAndRecordRegistrationAllowed(String ip);
}
