package com.thx.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.thx.common.util.UUIDUtil;
import com.thx.module.admin.pojo.LogMessage;
import com.thx.module.agent.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class CustomizeAppender extends AppenderBase<ILoggingEvent> {

    private static LogService logService;

    @Autowired
    public void setLogService(LogService logService) {
        CustomizeAppender.logService = logService;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (logService != null) {
            LogMessage logMessage = new LogMessage(
                    UUIDUtil.uuid(),
                    new Date(event.getTimeStamp()),
                    event.getLevel().toString(),
                    event.getLoggerName(),
                    event.getFormattedMessage()
            );
            logService.sendLogMessage(logMessage);
        }
    }
}
