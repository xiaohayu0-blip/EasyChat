package com.gym.easychatjava.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MomentCommentVO {

    private Long id;
    private UserVO user;          // 评论人
    private Long replyUserId;     // 被回复人ID(0=直接评论动态)
    private String replyUserName; // 被回复人昵称(直接评论时为空)
    private String content;
    private LocalDateTime createTime;

}
