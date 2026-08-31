package com.gym.easychatjava.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息返回结构(推送/回执给客户端用)
 */
@Data
public class MessageVO {
    private Long id;
    private Long fromUserId;
    private Long toId;
    private Integer contentType;
    private String content;
    private LocalDateTime createTime;
    /**0已发送 1已送达 2已读 3撤回*/
    private Integer status;
}
