package com.gym.easychatjava.service;

import com.gym.easychatjava.vo.ConversationVO;

import java.util.List;

public interface ConversationService {
    List<ConversationVO> listConversations();
}
