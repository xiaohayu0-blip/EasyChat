package com.gym.easychatjava.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MomentVO {

    private Long id;
    private UserVO user;               // 作者
    private String content;
    private List<String> images;       // 反序列化后的图片URL列表
    private LocalDateTime createTime;
    private int likeCount;             // 点赞数
    private boolean liked;             // 当前登录用户是否已点赞
    private List<MomentCommentVO> comments;

}
