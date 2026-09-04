package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.ConversationSettingDTO;
import com.gym.easychatjava.entity.ConversationSetting;
import com.gym.easychatjava.entity.Friend;
import com.gym.easychatjava.entity.Message;
import com.gym.easychatjava.entity.User;
import com.gym.easychatjava.mapper.ConversationSettingMapper;
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
    private final ConversationSettingMapper conversationSettingMapper;

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

        // 批量查我的会话设置(置顶/免打扰)
        Map<Long, ConversationSetting> settingMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            List<ConversationSetting> settings = conversationSettingMapper.selectList(
                    new LambdaQueryWrapper<ConversationSetting>()
                            .eq(ConversationSetting::getUserId, me)
                            .eq(ConversationSetting::getConversationType, 1)
                            .in(ConversationSetting::getPeerId, friendIds)
            );
            for (ConversationSetting s : settings) {
                settingMap.put(s.getPeerId(), s);
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

            ConversationSetting setting = settingMap.get(friendId);
            vo.setPinned(setting != null && setting.getPinned() == 1);
            vo.setMuted(setting != null && setting.getMuted() == 1);
            vo.setShowUnreadBadge(!vo.getMuted()&&vo.getUnreadCount()>0);

            result.add(vo);
        }

        // 置顶的排最前,其余按最后消息时间倒序
        result.sort((a, b) -> {
            boolean pa = Boolean.TRUE.equals(a.getPinned());
            boolean pb = Boolean.TRUE.equals(b.getPinned());
            if (pa != pb) {
                return pa ? -1 : 1;
            }
            return b.getLastTime().compareTo(a.getLastTime());
        });

        return result;
    }

    @Override
    public void setPinned(ConversationSettingDTO dto) {
        setFlag(dto,true);
    }

    @Override
    public void setMuted(ConversationSettingDTO dto) {
        setFlag(dto,false);
    }

    private void setFlag(ConversationSettingDTO dto,boolean isPin){
        Long me=UserContext.getUserId();

        // 1. 按唯一索引的三个字段查现有设置
        ConversationSetting setting=conversationSettingMapper.selectOne(
                new LambdaQueryWrapper<ConversationSetting>()
                        .eq(ConversationSetting::getUserId,me)
                        .eq(ConversationSetting::getConversationType,1)
                        .eq(ConversationSetting::getPeerId,dto.getPeerId())
        );

        // 2. Boolean 开关 → 存库的 0/1
        int value=dto.getEnabled()?1:0;

        if(setting==null){
            // 3. 没有这行:新建,另一个开关置 0
            setting=new ConversationSetting();
            setting.setUserId(me);
            setting.setConversationType(1);
            setting.setPeerId(dto.getPeerId());
            setting.setPinned(isPin?value:0);
            setting.setMuted(isPin?0:value);
            conversationSettingMapper.insert(setting);
        }else{
            // 4. 已有这行:只改对应的那一个开关,另一个保持原样
            if (isPin) {
                setting.setPinned(value);
            } else {
                setting.setMuted(value);
            }
            conversationSettingMapper.updateById(setting);
        }
    }
}
