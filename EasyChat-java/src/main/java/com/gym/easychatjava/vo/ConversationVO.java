package com.gym.easychatjava.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    /**对方用户ID*/
    private Long friendId;

    /**对方昵称*/
    private String friendNickname;

    /**对方头像URL*/
    private String friendAvatar;

    /**最后一条消息内容*/
    private String lastContent;

    /**最后一条消息类型*/
    private Integer lastContentType;

    /**最后消息时间*/
    private LocalDateTime lastTime;

    /**未读数*/
    private Integer unreadCount;

    /** 对方备注名(为空时前端回退显示昵称) */
    private String friendRemark;

    /**是否置顶*/
    private Boolean pinned;

    /**是否免打扰*/
    private Boolean muted;
}
