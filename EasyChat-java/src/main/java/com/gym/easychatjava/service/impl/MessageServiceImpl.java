package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.SendMessageDTO;
import com.gym.easychatjava.entity.Message;
import com.gym.easychatjava.mapper.MessageMapper;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final StringRedisTemplate stringRedisTemplate;

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

    @Override
    public List<MessageVO> listHistory(Long friendId, int page, int size) {

        Long me= UserContext.getUserId();

        // 边界保护:page 至少 1,size 限制在 1~100 之间
        if(page<1)page=1;
        if(size<1||size>100)size=20;

        // 计算偏移量:第 2 页 = 跳过前 20 条
        int offset=(page-1)*size;

        // 查双向单聊消息,按时间倒序,手动 LIMIT 分页
        List<Message> messages=messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationType,1)
                        .and(w->w
                                .eq(Message::getFromUserId,me).eq(Message::getToId,friendId)
                                .or()
                                .eq(Message::getFromUserId,friendId).eq(Message::getToId,me))
                        .orderByDesc(Message::getCreateTime)
                        .last("LIMIT"+offset+","+size)
        );

        // 拉历史 = 正在看这个会话,清掉该会话未读
        stringRedisTemplate.delete("unread:count:" + me + ":" + friendId);

        // Entity -> VO
        List<MessageVO> result = new ArrayList<>();
        for (Message m : messages) {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setFromUserId(m.getFromUserId());
            vo.setToId(m.getToId());
            vo.setContentType(m.getContentType());
            vo.setContent(m.getContent());
            vo.setCreateTime(m.getCreateTime()); // 这是 select 出来的,DB 有值,直接用
            result.add(vo);
        }
        return result;

    }

    @Override
    public int getUnreadCount(Long friendId) {
        Long me=UserContext.getUserId();
        String count=stringRedisTemplate.opsForValue().get("unread:count:"+me+":"+friendId);
        return count==null?0:Integer.parseInt(count);
    }
}
