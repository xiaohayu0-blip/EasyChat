package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送好友申请请求参数
 */
@Data
public class FriendRequestDTO {

    /** 被申请的用户ID */
    @NotNull(message = "对方用户ID不能为空")
    private Long toUserId;

    /** 验证消息,可不填 */
    @Size(max = 100, message = "验证消息最长100字")
    private String message;
}
