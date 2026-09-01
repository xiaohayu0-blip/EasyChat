package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.PublishMomentDTO;

public interface MomentService {

    void publish(PublishMomentDTO dto);

    void deleteMoment(Long momentId);

    void like(Long momentId);

    void unlike(Long momentId);
}
