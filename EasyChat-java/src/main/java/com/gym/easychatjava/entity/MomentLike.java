package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("moment_like")
public class MomentLike {

    @TableId(type= IdType.ASSIGN_ID)
    private Long id;

    private Long momentId;

    private Long userId;

    private LocalDateTime createTime;

}
