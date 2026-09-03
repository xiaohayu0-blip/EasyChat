package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.ConversationSettingDTO;
import com.gym.easychatjava.vo.ConversationVO;

import java.util.List;

public interface ConversationService {

    List<ConversationVO> listConversations();

    void setPinned(ConversationSettingDTO dto);

    void setMuted(ConversationSettingDTO dto);
}
