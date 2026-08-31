package com.gym.easychatjava.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.easychatjava.dto.SendMessageDTO;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器:管理在线连接
 * 本步骤先只做"连接/断开",发消息在 8.2 加 handleTextMessage
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    // 在线用户表:userId -> 该用户的 WebSocket 连接
    // 用 ConcurrentHashMap 是因为多个用户会并发建立/断开连接
    public static final Map<Long, WebSocketSession> ONLINE_SESSIONS=new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final StringRedisTemplate stringRedisTemplate;

    public ChatWebSocketHandler(ObjectMapper objectMapper, MessageService messageService, StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.messageService = messageService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

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
    /** 收到客户端发来的文本消息(JSON) */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws
            Exception {
        // 1. 拿发送者 userId(握手时存进 attributes 的)
        Long fromUserId=(Long)session.getAttributes().get("userId");

        // 2. 把客户端发来的 JSON 字符串解析成 SendMessageDTO 对象
        SendMessageDTO dto=objectMapper.readValue(message.getPayload(),SendMessageDTO.class);

        // 3. 落库并拿到带 id + createTime 的消息 VO
        MessageVO vo=messageService.sendMessage(fromUserId,dto);

        // 4. 序列化 VO 成 JSON,准备推送
        String json=objectMapper.writeValueAsString(vo);

        // 5. 推给接收方(只有在线才推,离线靠后面 8.3 拉历史)
        WebSocketSession toSession=ONLINE_SESSIONS.get(dto.getToUserId());
        if(toSession!=null&&toSession.isOpen()){
            toSession.sendMessage(new TextMessage(json));
        }else{
            // 接收方离线:Redis 未读数 +1(key 不存在时 INCR 会自动从 0 开始)
            stringRedisTemplate.opsForValue()
                    .increment("unread:count:"+dto.getToUserId()+":"+fromUserId);
        }

        // 6. 回执给发送方(让自己界面能立刻显示这条消息)
        session.sendMessage(new TextMessage(json));
    }

    /** 撤回通知:把撤回 VO 推给指定用户(供 HTTP 撤回接口调用) */
    public void pushRecall(Long toUserId,MessageVO recallVo){
        WebSocketSession toSession = ONLINE_SESSIONS.get(toUserId);
        if(toSession!=null&&toSession.isOpen()){
            try{
                toSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(recallVo)));
            }catch(Exception e){
                log.error("推送撤回通知失败,toUserId={}",toUserId,e);
            }
        }
    }
}
