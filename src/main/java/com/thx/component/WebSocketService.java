package com.thx.component;

import com.thx.module.admin.pojo.LogMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendLogMessage(LogMessage logMessage) {
        messagingTemplate.convertAndSend("/topic/logs", logMessage);
    }
}
