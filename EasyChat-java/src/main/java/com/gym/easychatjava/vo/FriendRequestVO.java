package com.gym.easychatjava.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestVO {
    private Long id;            // 申请id
    private Long fromUserId;    // 申请人id
    private String nickname;    // 申请人昵称
    private String avatar;      // 申请人头像
    private String message;     // 验证消息
    private LocalDateTime createTime;
}
