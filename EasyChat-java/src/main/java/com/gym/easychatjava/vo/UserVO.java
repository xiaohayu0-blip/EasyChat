package com.gym.easychatjava.vo;

import lombok.Data;

/**
 * 返回给前端的用户信息(不含密码等敏感字段)
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别:0未知 1男 2女 */
    private Integer gender;

    /** 个性签名 */
    private String signature;
}
