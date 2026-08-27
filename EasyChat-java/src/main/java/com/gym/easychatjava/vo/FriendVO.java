package com.gym.easychatjava.vo;

import lombok.Data;

@Data
public class FriendVO {
    private Long userId;        // 好友id
    private String nickname;
    private String avatar;
    private String remark;      // 备注名
}
