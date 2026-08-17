package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Integer conversationType;

    private Long fromUserId;

    private Long toId;

    private Integer contentType;

    private String content;

    private Integer status;

    private LocalDateTime createTime;
}
