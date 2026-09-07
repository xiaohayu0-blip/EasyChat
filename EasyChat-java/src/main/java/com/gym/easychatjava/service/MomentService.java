package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.CommentDTO;
import com.gym.easychatjava.dto.PublishMomentDTO;
import com.gym.easychatjava.vo.MomentVO;

import java.util.List;

public interface MomentService {

    void publish(PublishMomentDTO dto);

    void deleteMoment(Long momentId);

    void like(Long momentId);

    void unlike(Long momentId);

    void comment(CommentDTO dto);

    void deleteComment(Long commentId);

    List<MomentVO> listTimeline(int page, int size);

    MomentVO getMomentDetail(Long momentId);
    List<MomentVO> listUserMoments(Long userId,int page,int size);
}
