package com.gym.easychatjava.service.impl;

import com.gym.easychatjava.dto.SendMessageDTO;
import com.gym.easychatjava.entity.Message;
import com.gym.easychatjava.mapper.MessageMapper;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public MessageVO sendMessage(Long fromUserId, SendMessageDTO dto) {

        // 1. 组装实体并落库
        Message message = new Message();
        message.setConversationType(1); // 1 表示单聊(群聊暂不做)
        message.setFromUserId(fromUserId);
        message.setToId(dto.getToUserId());
        message.setContentType(dto.getContentType());
        message.setContent(dto.getContent());
        message.setStatus(0); // 0 已发送
        messageMapper.insert(message);

        // 2. 组装返回 VO
        MessageVO vo = new MessageVO();
        vo.setId(message.getId()); // insert 后 MyBatis-Plus 已回填雪花 ID
        vo.setFromUserId(fromUserId);
        vo.setToId(dto.getToUserId());
        vo.setContentType(dto.getContentType());
        vo.setContent(dto.getContent());
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }
}
