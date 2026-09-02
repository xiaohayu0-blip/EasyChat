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

    // TODO 学习:评论/回复 —— 校验动态存在、replyUserId 归一化 0、插入评论
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

    // TODO 学习:删除评论 —— 评论作者或动态作者可删、@TableLogic 逻辑删除
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

    // TODO 学习:好友时间线 —— 好友+自己动态分页倒序,IN 批量查避免 N+1,组装 MomentVO(作者/点赞数/是否已赞/评论)
    @Override
    public List<MomentVO> listTimeline(int page, int size) {
        Long me=UserContext.getUserId();

        //1.分页参数边界保护
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

        // 3. 查这些人的动态,时间倒序,手动 LIMIT 分页(和 listHistory 一样的写法)
        int offset=(page-1)*size;
        List<Moment> moments=momentMapper.selectList(
                new LambdaQueryWrapper<Moment>()
                        .in(Moment::getUserId,userIds)
                        .orderByDesc(Moment::getCreateTime)
                        .last("LIMIT"+offset+","+size)
                );
        if(moments.isEmpty()){
            return new ArrayList<>();
        }

        // 收集:所有动态id + 所有作者id
        List<Long> momentIds=new ArrayList<>();
        Set<Long> authorIds=new HashSet<>();
        for(Moment m:moments){
            momentIds.add(m.getId());
            authorIds.add(m.getUserId());
        }

        // 4. 批量查作者(id -> User)
        Map<Long, User>authorMap=new HashMap<>();
        List<User> authors=userMapper.selectBatchIds(authorIds);
        for(User u:authors){
            authorMap.put(u.getId(),u);
        }

        // 5. 一次查这些动态的所有点赞,统计:动态id -> 点赞数,以及"我点过赞的动态id集合"
        List<MomentLike> likes=momentLikeMapper.selectList(
                new LambdaQueryWrapper<MomentLike>().in(MomentLike::getMomentId,momentIds)
        );
        Map<Long,Integer> likeCountMap=new HashMap<>();
        Set<Long> myLikedIds=new HashSet<>();
        for (MomentLike like : likes) {
            likeCountMap.put(like.getMomentId(), likeCountMap.getOrDefault(like.getMomentId(), 0) + 1);
            if (like.getUserId().equals(me)) {
                myLikedIds.add(like.getMomentId());
            }
        }

        // 6. 一次查这些动态的所有评论,按动态分组;同时收集"评论人id + 被回复人id"
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

        // 批量查评论相关用户
        Map<Long, User> commentUserMap = new HashMap<>();
        if (!commentUserIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(commentUserIds);
            for (User u : users) {
                commentUserMap.put(u.getId(), u);
            }
        }

        // 7. 组装 VO
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

    // TODO 学习:图片 JSON 反序列化 —— TypeReference 保留泛型类型(泛型运行时擦除,List<String>.class 不存在)
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
