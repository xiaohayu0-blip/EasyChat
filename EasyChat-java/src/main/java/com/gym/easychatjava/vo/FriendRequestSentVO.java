package com.gym.easychatjava.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRequestSentVO {
    /**申请id*/
    private Long id;

    /**被申请人id*/
    private Long toUserId;

    /**对方昵称*/
    private String nickname;

    /**对方头像*/
    private String avatar;

    /**验证消息*/
    private String message;

    /**0待处理 1已同意 2已拒绝*/
    private Integer status;

    private LocalDateTime createTime;
}
