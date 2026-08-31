package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.FriendRequestDTO;
import com.gym.easychatjava.dto.HandleRequestDTO;
import com.gym.easychatjava.dto.UpdateRemarkDTO;
import com.gym.easychatjava.entity.Friend;
import com.gym.easychatjava.entity.FriendRequest;
import com.gym.easychatjava.entity.User;
import com.gym.easychatjava.mapper.FriendMapper;
import com.gym.easychatjava.mapper.FriendRequestMapper;
import com.gym.easychatjava.mapper.UserMapper;
import com.gym.easychatjava.service.FriendService;
import com.gym.easychatjava.vo.FriendRequestVO;
import com.gym.easychatjava.vo.FriendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class FriendServiceImpl implements FriendService {

    private final UserMapper userMapper;
    private final FriendMapper friendMapper;
    private final FriendRequestMapper friendRequestMapper;

    @Override
    public void sendRequest(FriendRequestDTO dto) {
        Long userId = UserContext.getUserId();
        Long toUserId = dto.getToUserId();

        //1.不能添加自己为好友
        if (userId.equals(toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能添加自己为好友");
        }

        //2.对方必须存在
        User toUser = userMapper.selectById(toUserId);
        if (toUser == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "对方用户不存在");
        }

        //3.是否已经是好友
        Long friendCount = friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, toUserId));
        if (friendCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "对方已是你的好友");
        }

        //4.是否已有待处理的申请(避免重复申请)
        Long requestCount = friendRequestMapper.selectCount(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getFromUserId, userId)
                        .eq(FriendRequest::getToUserId, toUserId)
                        .eq(FriendRequest::getStatus, 0));
        if (requestCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已发送过申请,请等待对方处理");
        }

        //5.插入申请记录,状态为0待处理
        FriendRequest request = new FriendRequest();
        request.setFromUserId(userId);
        request.setToUserId(toUserId);
        request.setMessage(dto.getMessage());
        request.setStatus(0);
        friendRequestMapper.insert(request);
    }

    @Override
    public List<FriendRequestVO> listReceivedRequests() {
        //1.拿当前登录用户id
        Long userId = UserContext.getUserId();

        //2.查发给我的、待处理的申请(按时间倒序)
        List<FriendRequest> requests = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getToUserId, userId)
                        .eq(FriendRequest::getStatus, 0)
                        .orderByDesc(FriendRequest::getCreateTime));

        //3.收集所有申请人的id
        List<Long> fromUserIds = new ArrayList<>();
        for (FriendRequest r : requests) {
            fromUserIds.add(r.getFromUserId());
        }

        //4.批量查申请人信息,放进Map(id -> user)
        Map<Long, User> userMap = new HashMap<>();
        if (!fromUserIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(fromUserIds);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        //5.组装VO列表
        List<FriendRequestVO> result = new ArrayList<>();
        for (FriendRequest r : requests) {
            User fromUser = userMap.get(r.getFromUserId());
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(r.getId());
            vo.setFromUserId(r.getFromUserId());
            vo.setMessage(r.getMessage());
            vo.setCreateTime(r.getCreateTime());
            if (fromUser != null) {
                vo.setNickname(fromUser.getNickname());
                vo.setAvatar(fromUser.getAvatar());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void handleRequest(Long requestId, HandleRequestDTO dto) {
        Long userId = UserContext.getUserId();

        //1.查申请
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "申请不存在");
        }

        //2.权限校验:这条申请必须是发给我的
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无权处理该申请");
        }

        //3.状态校验:只有待处理(0)才能处理
        if (request.getStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该申请已处理");
        }

        //4.更新申请状态 + 处理时间
        request.setStatus(dto.getStatus());
        request.setHandleTime(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        //5.同意时,双向插入两条好友记录
        if (dto.getStatus() == 1) {
            Friend f1 = new Friend();
            f1.setUserId(request.getFromUserId());   // 申请人视角
            f1.setFriendId(request.getToUserId());
            friendMapper.insert(f1);

            Friend f2 = new Friend();
            f2.setUserId(request.getToUserId());     // 被申请人视角
            f2.setFriendId(request.getFromUserId());
            friendMapper.insert(f2);
        }
    }

    @Override
    public List<FriendVO> listFriends() {
        Long userId = UserContext.getUserId();

        //1.查我的好友关系
        List<Friend> friends = friendMapper.selectList(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId));

        //2.收集所有好友id
        List<Long> friendIds=new ArrayList<>();
        for (Friend f : friends) {
            friendIds.add(f.getFriendId());
        }

        //3.批量查好友信息,放进Map
        Map<Long, User> userMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(friendIds);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        //4.组装VO
        List<FriendVO> result=new ArrayList<>();
        for(Friend f:friends){
            User friendUser = userMap.get(f.getFriendId());
            FriendVO vo=new FriendVO();
            vo.setUserId(f.getFriendId());
            vo.setRemark(f.getRemark());
            if(friendUser!=null){
                vo.setNickname(friendUser.getNickname());
                vo.setAvatar(friendUser.getAvatar());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void deleteFriend(Long friendId) {
        Long userId = UserContext.getUserId();

        //物理删除两条关系:(我,对方) 和 (对方,我)
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId));

        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId));
    }

    @Override
    public void updateRemark(UpdateRemarkDTO dto) {
        Long me=UserContext.getUserId();

        // 1. 查"我 → 对方"这条好友关系(只查自己这条,不查反向)
        Friend friend=friendMapper.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId,me)
                        .eq(Friend::getFriendId,dto.getFriendId())
        );
        if(friend==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"你们还不是好友");
        }

        // 2. 归一化:null 或纯空白都存成 ""(表示无备注)
        String remark=dto.getRemark()==null?"":dto.getRemark().trim();
        friend.setRemark(remark);
        friendMapper.updateById(friend);
    }
}
