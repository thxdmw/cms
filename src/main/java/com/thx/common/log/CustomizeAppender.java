package com.thx.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.thx.common.util.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义 Logback Appender：拦截应用运行时产生的每一条日志，转成 {@link LogMessage}
 * 并交给 {@link LogMessagePublisher} 推送出去（目前的实现是 WebSocket 广播给后台日志页面）。
 * <p>
 * 只依赖 common 包内的 {@link LogMessagePublisher} 接口，不直接依赖具体的业务 module，
 * 真正的推送实现由 Spring 在运行时注入（见 module.agent.service.LogService）。
 * logService 是 static 字段：Logback Appender 的实例由 Logback 框架自己创建、生命周期早于
 * Spring 容器，无法用常规的构造器/字段注入，只能等 Spring 启动后通过 setter 回填。
 */
@Slf4j
@Component
public class CustomizeAppender extends AppenderBase<ILoggingEvent> {

    private static LogMessagePublisher logMessagePublisher;

    @Autowired
    public void setLogMessagePublisher(LogMessagePublisher logMessagePublisher) {
        CustomizeAppender.logMessagePublisher = logMessagePublisher;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (logMessagePublisher != null) {
            LogMessage logMessage = new LogMessage(
                    UUIDUtil.uuid(),
                    new Date(event.getTimeStamp()),
                    event.getLevel().toString(),
                    event.getLoggerName(),
                    event.getFormattedMessage()
            );
            logMessagePublisher.sendLogMessage(logMessage);
        }
    }
}
