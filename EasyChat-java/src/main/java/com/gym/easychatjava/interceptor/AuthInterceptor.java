package com.gym.easychatjava.interceptor;

import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor{

    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public AuthInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler)throws Exception{
        //1.从请求头获取token
        String token = request.getHeader("token");

        //2.校验token是否存在
        if(token == null ||token.isBlank()){
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        //3.从reids中获取用户ID
        String userIdStr = stringRedisTemplate.opsForValue().get(token);
        if(userIdStr == null){
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        //4.将用户ID存入线程上下文
        UserContext.setUserId(Long.valueOf(userIdStr));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception ex)throws Exception{
        //清理线程上下文,防止内存泄露
        UserContext.remove();
    }
}
