package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.SendMessageDTO;
import com.gym.easychatjava.vo.MessageVO;

import java.util.List;

public interface MessageService {

    /** 发送消息:落库并返回带 id 和 createTime 的消息 VO */
    MessageVO sendMessage(Long fromUserId, SendMessageDTO dto);

    /** 拉取与某好友的历史消息(倒序分页) */
    List<MessageVO> listHistory(Long friendId, int page, int size);
}
