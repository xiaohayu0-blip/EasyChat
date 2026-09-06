package com.gym.easychatjava.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.dto.SendMessageDTO;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import com.gym.easychatjava.vo.WsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
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

        MessageVO vo;
        try {
            vo = messageService.sendMessage(fromUserId, dto);
        } catch (BusinessException e) {
            // 发送失败(如被拉黑):构造一个 status=4 的失败回执,只回给发送方自己
            MessageVO failVo = new MessageVO();
            failVo.setFromUserId(fromUserId);
            failVo.setToId(dto.getToUserId());
            failVo.setContentType(dto.getContentType());
            failVo.setContent(e.getMessage());   // 错误提示,来自 BusinessException
            failVo.setStatus(4);
            failVo.setCreateTime(LocalDateTime.now());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(failVo)));
            return;   // 关键:失败就到此为止,不落库、不推送给对方
        }

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

    /**好友申请实时推送事件*/
    public void pushEvent(Long toUserId,String type,Object data){

        // 1. 从在线表拿接收方的 WebSocketSession
        WebSocketSession toSession = ONLINE_SESSIONS.get(toUserId);

        // 2. 判空 + isOpen()(离线就不用推了,和 pushRecall 一致)
        if(toSession!=null&&toSession.isOpen()){
            // 3. new 一个 WsMessage,setType(type)、setData(data)
            WsMessage<Object> wsMessage = new WsMessage<>();
            wsMessage.setType(type);
            wsMessage.setData(data);

            // 4. objectMapper.writeValueAsString(wsMessage) 序列化成 JSON
            // 5. new TextMessage(json) 发送
            // 6. try/catch 包住,失败用 log.error,别让一个推送失败打断整个流程
            try {
                toSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsMessage)));
            } catch (IOException e) {
                log.error("推送好友申请通知失败,toUserId={}",toUserId,e);
            }
        }

    }
}
