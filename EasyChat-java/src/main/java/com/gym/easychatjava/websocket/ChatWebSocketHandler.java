package com.gym.easychatjava.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器:管理在线连接
 * 本步骤先只做"连接/断开",发消息在 8.2 加 handleTextMessage
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    // 在线用户表:userId -> 该用户的 WebSocket 连接
    // 用 ConcurrentHashMap 是因为多个用户会并发建立/断开连接
    public static final Map<Long, WebSocketSession> ONLINE_SESSIONS=new ConcurrentHashMap<>();

    /** 连接建立后:把该用户注册到在线表 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        Long userId=(Long)session.getAttributes().get("userId");
        if(userId!=null){
            ONLINE_SESSIONS.put(userId,session);
        }
    }

    /** 连接断开后:从在线表移除 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        Long userId =(Long) session.getAttributes().get("userId");
        if(userId!=null){
            ONLINE_SESSIONS.remove(userId);
        }
    }
}
