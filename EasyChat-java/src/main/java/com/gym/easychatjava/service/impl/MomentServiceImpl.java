package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.PublishMomentDTO;
import com.gym.easychatjava.entity.Moment;
import com.gym.easychatjava.entity.MomentLike;
import com.gym.easychatjava.mapper.MomentLikeMapper;
import com.gym.easychatjava.mapper.MomentMapper;
import com.gym.easychatjava.service.MomentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MomentServiceImpl implements MomentService {

    private final MomentMapper momentMapper;

    private final ObjectMapper objectMapper;

    private final MomentLikeMapper momentLikeMapper;

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
}
