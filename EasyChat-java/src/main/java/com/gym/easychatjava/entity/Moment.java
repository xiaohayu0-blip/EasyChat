package com.gym.easychatjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("moment")
public class Moment {

    /** 主键,雪花ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 发布者用户ID */
    private Long userId;

    /** 文字内容,只发图时为空串 */
    private String content;

    /** 图片URL的JSON数组 */
    private String images;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer deleted;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
