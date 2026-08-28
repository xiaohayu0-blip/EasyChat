package com.gym.easychatjava.config;

import com.gym.easychatjava.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置:开启 WebSocket,把 ChatWebSocketHandler 注册到 /ws 路径
 */
@Configuration
@EnableWebSocket//开启 Spring 的 WebSocket 支持(没有它,WebSocketConfigurer 不生效)
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler,"/ws")
                .addInterceptors(handshakeInterceptor)// 握手时走我们的 token 校验
                .setAllowedOrigins("*");// 允许所有来源(开发期用)
    }
}
