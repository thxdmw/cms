package com.thx.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
//@ConfigurationProperties(prefix = "websocket")
//@Data
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("#{'${websocket.allowedOrigins}'.split(',')}")
    private List<String> allowedOrigins;

    //private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("允许域名：{}", allowedOrigins);
        registry.addEndpoint("/websocket-logs")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .withSockJS();
    }

    // 确保在WebSocketConfig中配置了心跳机制和连接超时
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setSendTimeLimit(15 * 1000)
                .setSendBufferSizeLimit(512 * 1024)
                .setMessageSizeLimit(128 * 1024);
    }
}
