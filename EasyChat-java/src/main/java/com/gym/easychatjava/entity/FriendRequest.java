package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_request")
public class FriendRequest {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**发送申请的用户id*/
    private Long fromUserId;

    /**被申请的用户id*/
    private Long toUserId;

    /**验证消息*/
    private String message;

    /**0待处理 1已同意 2已拒绝*/
    private Integer status;

    /**处理时间*/
    private LocalDateTime handleTime;

    /**逻辑删除:0未删 1已删*/
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
