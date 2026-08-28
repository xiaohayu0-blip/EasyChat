package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * WebSocket 发送消息请求参数(客户端通过 WebSocket 发来的 JSON)
 */
@Data
public class SendMessageDTO {
    /** 接收方用户ID */
    @NotNull(message = "接收方ID不能为空")
    private Long toUserId;

    /** 消息类型:1文本 2图片 3语音 4视频 5文件 */
    @NotNull(message = "消息类型不能为空")
    private Integer contentType;

    /** 消息内容(文本或媒体URL) */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
