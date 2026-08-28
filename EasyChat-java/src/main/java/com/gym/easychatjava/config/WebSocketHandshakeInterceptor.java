package com.gym.easychatjava.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器:在建立连接前校验token
 */
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public WebSocketHandshakeInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 握手前调用:认证token,把userId塞进attributes,供后面的Handler使用
     * 返回false会拒绝握手
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes){
        //1.WebSocket不能带请求头,token从url query里取:ws?token=xxx
        ServletServerHttpRequest servletRequest=(ServletServerHttpRequest) request;
        String token=servletRequest.getServletRequest().getParameter("token");

        //2.没带token直接拒绝
        if(token==null||token.isBlank()){
            return false;
        }

        //3.查Redis,和登录时一样:token作key,value是userId
        String userIdStr=stringRedisTemplate.opsForValue().get(token);
        if(userIdStr==null){
            return false;
        }

        //4.把userId存进attributes.注意:不能用UserContext(ThreadLocal),因为WebSocket收消息的线程和握手线程不是同一个,ThreadLocal取不到值
        attributes.put("userId",Long.valueOf(userIdStr));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
