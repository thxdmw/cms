package com.thx.common.log;

/**
 * 日志消息推送的最小抽象。{@link CustomizeAppender} 只依赖这个接口，不感知具体是谁在消费日志
 * （目前的实现是 module.agent 把日志通过 WebSocket 广播给后台日志页面），
 * 避免 common 包反过来依赖具体业务 module，保持依赖方向由外向内。
 */
public interface LogMessagePublisher {

    /**
     * 发布一条日志消息
     */
    void sendLogMessage(LogMessage logMessage);

}
