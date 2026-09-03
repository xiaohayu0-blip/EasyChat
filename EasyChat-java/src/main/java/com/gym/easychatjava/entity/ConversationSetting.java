package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_setting")
public class ConversationSetting {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /**会话类型:1单聊 2群聊(预留)*/
    private Integer conversationType;

    /**对方ID:单聊为好友ID,群聊为群ID*/
    private Long peerId;

    /**是否置顶:0否 1是*/
    private Integer pinned;

    /**是否免打扰:0否 1是*/
    private Integer muted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
