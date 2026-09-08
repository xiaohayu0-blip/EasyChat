package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.CommentDTO;
import com.gym.easychatjava.dto.PublishMomentDTO;
import com.gym.easychatjava.entity.*;
import com.gym.easychatjava.mapper.*;
import com.gym.easychatjava.service.MomentService;
import com.gym.easychatjava.vo.MomentCommentVO;
import com.gym.easychatjava.vo.MomentVO;
import com.gym.easychatjava.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
public class MomentServiceImpl implements MomentService {

    private final MomentMapper momentMapper;

    private final ObjectMapper objectMapper;

    private final MomentLikeMapper momentLikeMapper;

    private final UserMapper userMapper;// 校验"被回复的用户"是否存在

    private final MomentCommentMapper momentCommentMapper;// 评论表的增删

    private final FriendMapper friendMapper;   // 查我的好友

    @Override
    public void publish(PublishMomentDTO dto) {

        //拿当前登录用户
        Long userId = UserContext.getUserId();

        boolean contentEmpty = dto.getContent() == null || dto.getContent().isBlank();
        boolean imagesEmpty  = dto.getImages() == null || dto.getImages().isEmpty();
        if (contentEmpty && imagesEmpty) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "动态内容不能为空");
        }

        String imagesJson;
        try {
            imagesJson = objectMapper.writeValueAsString(dto.getImages()); // ["a","b"]
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "图片处理失败");
        }

        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(dto.getContent() == null ? "" : dto.getContent());
        moment.setImages(imagesJson);
        momentMapper.insert(moment);
    }

    @Override
    public void deleteMoment(Long momentId) {

        Moment moment=momentMapper.selectById(momentId);
        Long userId = UserContext.getUserId();

        if(moment==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"动态不存在");
        }

        if(!moment.getUserId().equals(userId)){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"只能删除自己的动态");
        }

        momentMapper.deleteById(momentId);
    }

    @Override
    public void like(Long momentId) {
        Long userId = UserContext.getUserId();
        if(momentMapper.selectById(momentId)==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"动态不存在");
        }

        if(momentLikeMapper.selectCount(
                new LambdaQueryWrapper<MomentLike>()
                        .eq(MomentLike::getMomentId, momentId)
                        .eq(MomentLike::getUserId, userId)) > 0){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"已经点过赞了");
        }

        MomentLike like = new MomentLike();
        like.setMomentId(momentId);
        like.setUserId(userId);
        momentLikeMapper.insert(like);
    }

    @Override
    public void unlike(Long momentId) {

        Long userId = UserContext.getUserId();

        if(momentMapper.selectById(momentId)==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"动态不存在");
        }

        momentLikeMapper.delete(
                new LambdaQueryWrapper<MomentLike>()
                        .eq(MomentLike::getMomentId, momentId)
                        .eq(MomentLike::getUserId, userId)
        );
    }

    @Override
    public void comment(CommentDTO dto) {

        Long userId=UserContext.getUserId();

        // 1. 动态必须存在
        if(momentMapper.selectById(dto.getMomentId())==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "动态不存在");
        }

        // 2. 回复时被回复的用户必须存在;直接评论则归一化成 0 存库
        Long replyUserId=dto.getReplyUserId()==null?0:dto.getReplyUserId();
        if(replyUserId!=0&&userMapper.selectById(replyUserId)==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "被回复的用户不存在");
        }

        // 3. 插入评论
        MomentComment comment = new MomentComment();
        comment.setMomentId(dto.getMomentId());
        comment.setUserId(userId);        // 评论人 = 当前登录用户
        comment.setReplyUserId(replyUserId);
        comment.setContent(dto.getContent());
        momentCommentMapper.insert(comment);
    }

    @Override
    public void deleteComment(Long commentId) {
        Long me=UserContext.getUserId();

        // 1. 评论必须存在(@TableLogic 会自动把"已删"的过滤掉,查不到 = 不存在)
        MomentComment comment=momentCommentMapper.selectById(commentId);
        if(comment==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "评论不存在");
        }

        // 2. 找到这条评论所在的动态,用于判断"动态作者"权限
        Moment moment=momentMapper.selectById(comment.getMomentId());
        if(moment==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "动态不存在");
        }

        // 3. 权限:评论作者 或 动态作者 才能删(复刻微信:作者删自己评论,动态主人删别人评论)
        boolean isCommentAuthor = comment.getUserId().equals(me);
        boolean isMomentAuthor = moment.getUserId().equals(me);
        if (!isCommentAuthor && !isMomentAuthor) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无权删除该评论");
        }

        // 4. 逻辑删除(会触发 @TableLogic,实际是 UPDATE deleted=1)
        momentCommentMapper.deleteById(commentId);

    }

    @Override
    public List<MomentVO> listTimeline(int page, int size) {
        Long me=UserContext.getUserId();

        // 1. 分页参数边界保护
        if(page<1)page=1;
        if(size<1||size>50)size=10;

        // 2. 查我的好友id列表
        List<Friend> friends=friendMapper.selectList(
                new LambdaQueryWrapper<Friend>().eq(Friend::getUserId,me)
        );
        List<Long> userIds=new ArrayList<>();
        userIds.add(me);
        for(Friend f:friends){
            userIds.add(f.getFriendId());
        }

        // 3. 查这些人的动态,时间倒序,手动 LIMIT 分页
        int offset=(page-1)*size;
        List<Moment> moments=momentMapper.selectList(
                new LambdaQueryWrapper<Moment>()
                        .in(Moment::getUserId,userIds)
                        .orderByDesc(Moment::getCreateTime)
                        .last("LIMIT " + offset + ","+ size)
                );

        // 4. 交给公共方法组装
        return buildMomentVOList(moments, me);
    }

    @Override
    public MomentVO getMomentDetail(Long momentId) {
        Long me=UserContext.getUserId();

        // 1. 动态必须存在
        Moment moment=momentMapper.selectById(momentId);
        if(moment==null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "动态不存在");
        }

        // 2. 权限:只能看「自己」或「好友」的动态(复刻微信:陌生人点不进你朋友圈)
        Long authorId=moment.getUserId();
        boolean isMine=authorId.equals(me);
        boolean isFriend=friendMapper.selectCount(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId,me)
                        .eq(Friend::getFriendId,authorId)
        )>0;
        if(!isMine&&!isFriend){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无权查看该动态");
        }

        // 3. 复用公共方法:单条包成 List 传进去,取回第 0 个
        return buildMomentVOList(Collections.singletonList(moment), me).get(0);
    }

    @Override
    public List<MomentVO> listUserMoments(Long userId, int page, int size) {
        Long me = UserContext.getUserId();

        // 1. 权限:只能看自己或好友
        if(!userId.equals(me)){
            boolean isFriend=friendMapper.selectCount(
                    new LambdaQueryWrapper<Friend>()
                            .eq(Friend::getUserId,me)
                            .eq(Friend::getFriendId,userId)
            )>0;
            if(!isFriend){
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"无权查看该用户的朋友圈");
            }
        }

        // 2. 分页边界
        if(page<1)page=1;
        if(size<1||size>50)size=10;

        // 3. 查该用户的动态,时间倒序
        int offset=(page-1)*size;
        List<Moment> moments=momentMapper.selectList(
                new LambdaQueryWrapper<Moment>()
                        .eq(Moment::getUserId,userId)
                        .orderByDesc(Moment::getCreateTime)
                        .last("LIMIT " + offset + "," + size)
        );

        // 4. 交给公共方法组装
        return buildMomentVOList(moments, me);
    }

    /**
     * 公共组装方法:给一批动态 批量查作者/点赞/评论并组装成 VO。
     * listTimeline、listUserMoments、getMomentDetail 三处共用,避免重复代码。
     */
    private List<MomentVO> buildMomentVOList(List<Moment> moments, Long me) {
        if (moments == null || moments.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集:所有动态id + 所有作者id
        List<Long> momentIds = new ArrayList<>();
        Set<Long> authorIds = new HashSet<>();
        for (Moment m : moments) {
            momentIds.add(m.getId());
            authorIds.add(m.getUserId());
        }

        // 批量查作者(id -> User)
        Map<Long, User> authorMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(authorIds)) {
            authorMap.put(u.getId(), u);
        }

        // 查这些动态的所有点赞:统计点赞数 + 我是否赞过 + 点赞人列表
        List<MomentLike> likes = momentLikeMapper.selectList(
                new LambdaQueryWrapper<MomentLike>()
                        .in(MomentLike::getMomentId, momentIds)
                        .orderByAsc(MomentLike::getCreateTime));
        Map<Long, Integer> likeCountMap = new HashMap<>();
        Set<Long> myLikedIds = new HashSet<>();
        Map<Long, List<Long>> likeUserIdsMap = new HashMap<>();   // 动态id -> 点赞人id列表
        Set<Long> likeUserIds = new HashSet<>();                    // 所有点赞人id(去重)
        for (MomentLike like : likes) {
            likeCountMap.put(like.getMomentId(), likeCountMap.getOrDefault(like.getMomentId(), 0) + 1);
            likeUserIdsMap.computeIfAbsent(like.getMomentId(), k -> new ArrayList<>()).add(like.getUserId());
            likeUserIds.add(like.getUserId());
            if (like.getUserId().equals(me)) {
                myLikedIds.add(like.getMomentId());
            }
        }
        Map<Long, User> likeUserMap = new HashMap<>();
        if (!likeUserIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(likeUserIds)) {
                likeUserMap.put(u.getId(), u);
            }
        }

        // 查这些动态的所有评论,按动态分组;同时收集评论人/被回复人id
        List<MomentComment> comments = momentCommentMapper.selectList(
                new LambdaQueryWrapper<MomentComment>()
                        .in(MomentComment::getMomentId, momentIds)
                        .orderByAsc(MomentComment::getCreateTime));
        Map<Long, List<MomentComment>> commentMap = new HashMap<>();
        Set<Long> commentUserIds = new HashSet<>();
        for (MomentComment c : comments) {
            commentMap.computeIfAbsent(c.getMomentId(), k -> new ArrayList<>()).add(c);
            commentUserIds.add(c.getUserId());
            if (c.getReplyUserId() != 0) {
                commentUserIds.add(c.getReplyUserId());
            }
        }
        Map<Long, User> commentUserMap = new HashMap<>();
        if (!commentUserIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(commentUserIds)) {
                commentUserMap.put(u.getId(), u);
            }
        }

        // 组装 VO
        List<MomentVO> result = new ArrayList<>();
        for (Moment m : moments) {
            MomentVO vo = new MomentVO();
            vo.setId(m.getId());
            vo.setContent(m.getContent());
            vo.setImages(parseImages(m.getImages()));              // JSON 字符串 -> List
            vo.setCreateTime(m.getCreateTime());
            vo.setUser(UserVO.from(authorMap.get(m.getUserId())));
            vo.setLikeCount(likeCountMap.getOrDefault(m.getId(), 0));
            vo.setLiked(myLikedIds.contains(m.getId()));

            // 点赞人列表(按点赞先后)
            List<UserVO> likeUserVOs = new ArrayList<>();
            List<Long> luids = likeUserIdsMap.get(m.getId());
            if (luids != null) {
                for (Long uid : luids) {
                    User u = likeUserMap.get(uid);
                    if (u != null) {
                        likeUserVOs.add(UserVO.from(u));
                    }
                }
            }
            vo.setLikeUsers(likeUserVOs);

            // 组装该动态的评论
            List<MomentCommentVO> commentVOs = new ArrayList<>();
            List<MomentComment> cs = commentMap.get(m.getId());
            if (cs != null) {
                for (MomentComment c : cs) {
                    MomentCommentVO cvo = new MomentCommentVO();
                    cvo.setId(c.getId());
                    cvo.setContent(c.getContent());
                    cvo.setCreateTime(c.getCreateTime());
                    cvo.setUser(UserVO.from(commentUserMap.get(c.getUserId())));
                    cvo.setReplyUserId(c.getReplyUserId());
                    // 回复人昵称:0=直接评论(空),非 0 查昵称
                    if (c.getReplyUserId() != 0 && commentUserMap.get(c.getReplyUserId()) != null) {
                        cvo.setReplyUserName(commentUserMap.get(c.getReplyUserId()).getNickname());
                    }
                    commentVOs.add(cvo);
                }
            }
            vo.setComments(commentVOs);
            result.add(vo);
        }
        return result;
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
