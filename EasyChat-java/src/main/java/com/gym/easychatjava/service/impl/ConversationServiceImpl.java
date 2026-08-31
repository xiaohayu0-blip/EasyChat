package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.entity.Friend;
import com.gym.easychatjava.entity.Message;
import com.gym.easychatjava.entity.User;
import com.gym.easychatjava.mapper.FriendMapper;
import com.gym.easychatjava.mapper.MessageMapper;
import com.gym.easychatjava.mapper.UserMapper;
import com.gym.easychatjava.service.ConversationService;
import com.gym.easychatjava.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final FriendMapper friendMapper;

    @Override
    public List<ConversationVO> listConversations() {

        Long me= UserContext.getUserId();

        List<Message> latest=messageMapper.listLatestMessages(me);

        //归一化出对方id并收集
        List<Long> friendIds=new ArrayList<>();
        for(Message message:latest){
            Long friendId=message.getFromUserId().equals(me)
                    ?message.getToId()
                    :message.getFromUserId();
            friendIds.add(friendId);
        }

        //批量查用户
        Map<Long, User> userMap=new HashMap<>();
        if(!friendIds.isEmpty()){
            List<User> users=userMapper.selectBatchIds(friendIds);
            for(User u:users)userMap.put(u.getId(),u);
        }

        // 批量查我设置的好友备注
        Map<Long,String> remarkMap=new HashMap<>();
        if(!friendIds.isEmpty()){
            List<Friend> friends=friendMapper.selectList(
                    new LambdaQueryWrapper<Friend>()
                            .eq(Friend::getUserId,me)
                            .in(Friend::getFriendId,friendIds)
            );
            for(Friend f:friends){
                remarkMap.put(f.getFriendId(),f.getRemark());
            }
        }

        //组装VO列表
        List<ConversationVO> result=new ArrayList<>();
        for(Message message:latest){

            //再算一次对方id
            Long friendId=message.getFromUserId().equals(me)
                    ?message.getToId()
                    :message.getFromUserId();

            // 5.2 从 userMap 里取对方用户;可能是 null(对方注销了),所以要判空
            User friendUser=userMap.get(friendId);

            // 5.3 new 一个 VO,把 message 上现成的字段先填进去
            ConversationVO vo = new ConversationVO();
            vo.setFriendId(friendId);
            vo.setFriendRemark(remarkMap.get(friendId));
            vo.setLastContent(message.getContent());
            vo.setLastContentType(message.getContentType());
            vo.setLastTime(message.getCreateTime());

            // 5.4 昵称/头像来自 user 表,判空后再填
            if (friendUser != null) {
                vo.setFriendNickname(friendUser.getNickname());
                vo.setFriendAvatar(friendUser.getAvatar());
            }

            // 5.5 未读数:从 Redis 读,读不到就是 0
            String count = stringRedisTemplate.opsForValue()
                    .get("unread:count:" + me + ":" + friendId);
            vo.setUnreadCount(count == null ? 0 : Integer.parseInt(count));

            result.add(vo);
        }
        return result;
    }
}
