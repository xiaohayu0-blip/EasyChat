package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.SendMessageDTO;
import com.gym.easychatjava.entity.Friend;
import com.gym.easychatjava.entity.Message;
import com.gym.easychatjava.mapper.FriendMapper;
import com.gym.easychatjava.mapper.MessageMapper;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final FriendMapper friendMapper;

    @Override
    public MessageVO sendMessage(Long fromUserId, SendMessageDTO dto) {

        //校验我有没有拉黑对方
        Long blockedByMe=friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId,fromUserId)
                        .eq(Friend::getFriendId,dto.getToUserId())
                        .eq(Friend::getBlocked,1)
        );
        if(blockedByMe>0){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"你已拉黑对方,无法发送消息");
        }

        //校验对方有没有拉黑我
        Long blockedByPeer=friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId,dto.getToUserId())
                        .eq(Friend::getFriendId,fromUserId)
                        .eq(Friend::getBlocked,1)
        );
        if(blockedByPeer>0){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"消息已发出,但对方拒收了");
        }

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
        vo.setStatus(0);
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
                                .eq(Message::getFromUserId,me).eq(Message::getToId,friendId).eq(Message::getDeletedBySender,0)
                                .or()
                                .eq(Message::getFromUserId,friendId).eq(Message::getToId,me).eq(Message::getDeletedByReceiver,0))
                        .orderByDesc(Message::getCreateTime)
                        .last("LIMIT " + offset + "," + size)
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
            vo.setStatus(m.getStatus());
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

    @Override
    public MessageVO recall(Long messageId) {
        Long me=UserContext.getUserId();

        // 1. 按主键查消息
        Message message=messageMapper.selectById(messageId);
        if(message==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"消息不存在");
        }

        // 2. 只能撤回自己发的
        if(!message.getFromUserId().equals(me)){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"只能撤回自己发的消息");
        }

        // 3. 超 2 分钟不能撤回
        if(message.getCreateTime().plusMinutes(2).isBefore(LocalDateTime.now())){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"超过2分钟的消息不能撤回");
        }

        // 4. 标记已撤回并更新
        message.setStatus(3);
        messageMapper.updateById(message);

        // 5. 组装 VO(Controller 拿它去推送)
        MessageVO vo=new MessageVO();
        vo.setId(message.getId());
        vo.setFromUserId(message.getFromUserId());
        vo.setToId(message.getToId());
        vo.setContentType(message.getContentType());
        vo.setContent(null);
        vo.setCreateTime(message.getCreateTime());
        vo.setStatus(3);
        return vo;
    }

    @Transactional
    @Override
    public void deleteMessages(List<Long> messageIds) {

        Long me = UserContext.getUserId();

        for(Long id:messageIds){
            Message message = messageMapper.selectById(id);

            if(message == null){
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "消息不存在");
            }

            if(message.getFromUserId().equals(me)){
                message.setDeletedBySender(1);
            }else if(message.getToId().equals(me)){
                message.setDeletedByReceiver(1);
            }else {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无权删别人的会话消息");
            }

            messageMapper.updateById(message);
        }
    }
}
