package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    /** 主键,雪花ID **/
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 手机号,登陆账号 */
    private String phone;

    /** BCrypt加密后的密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别:0未知 1男 2女 */
    private Integer gender;

    /** 个性签名 */
    private String signature;

    /** 账号状态:0正常 1禁用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;


}
