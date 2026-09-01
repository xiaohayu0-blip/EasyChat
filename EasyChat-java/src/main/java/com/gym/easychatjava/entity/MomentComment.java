package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("moment_comment")
public class MomentComment {

    @TableId(type= IdType.ASSIGN_ID)
    private Long id;

    private Long momentId;

    private Long userId;

    private Long replyUserId;

    private String content;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

}
