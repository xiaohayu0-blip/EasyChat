package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentDTO {

    @NotNull(message = "动态ID不能为空")
    private Long momentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max=500,message = "评论最多500字")
    private String content;

    // 被回复的用户ID:null 或 0 表示直接评论动态,非 0 表示回复某人
    private Long replyUserId;
}
