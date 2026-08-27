package com.gym.easychatjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.common.UserContext;
import com.gym.easychatjava.dto.LoginDTO;
import com.gym.easychatjava.dto.RegisterDTO;
import com.gym.easychatjava.dto.UpdatePasswordDTO;
import com.gym.easychatjava.dto.UpdateUserDTO;
import com.gym.easychatjava.entity.User;
import com.gym.easychatjava.mapper.UserMapper;
import com.gym.easychatjava.service.UserService;
import com.gym.easychatjava.vo.LoginVO;
import com.gym.easychatjava.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        //1.手机号查重
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone,registerDTO.getPhone()));
        if(count>0){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"手机号已被注册");
        }

        String nickname = registerDTO.getNickname();
        if(nickname==null||nickname.isBlank()){
            nickname="用户"+registerDTO.getPhone().substring(7);
        }

        //2.组装User对象,密码必须加密
        User user = new User();
        user.setPhone(registerDTO.getPhone());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(nickname);

        //3.入库
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        //1.按手机号查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone,loginDTO.getPhone()));
        if(user == null){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"手机号或密码错误");

        }

        //2.校验密码
        if(!passwordEncoder.matches(loginDTO.getPassword(),user.getPassword())){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"手机号或密码错误");
        }

        //3.校验账号是否被禁用
        if(user.getStatus()!=null&&user.getStatus()==1){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"账号已被禁用");
        }

        //4.生成token并存入Redis
        String token = UUID.randomUUID().toString().replace("-","");
        stringRedisTemplate.opsForValue().set(token,String.valueOf(user.getId()), Duration.ofDays(7));

        //5.更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        //6.组装返回LoginVO

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(UserVO.from(user));
        return loginVO;
    }

    @Override
    public UserVO getMyInfo() {
        //1.从线程上下文拿当前登录用户ID(登录时拦截器已把userId存入ThreadLocal)
        Long userId = UserContext.getUserId();
        //2.按主键查用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        //3.实体转VO,隐藏敏感字段后返回
        return UserVO.from(user);
    }

    @Override
    public UserVO updateMyInfo(UpdateUserDTO dto) {
        Long id = UserContext.getUserId();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }

        // 只更新 DTO 里传了值的字段(null 表示"不改")
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }

        userMapper.updateById(user);
        return UserVO.from(user);
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto) {
        Long id = UserContext.getUserId();
        User user = userMapper.selectById(id);
        String oldPassword=dto.getOldPassword();
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }

        if(!passwordEncoder.matches(oldPassword,user.getPassword())){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"密码错误");
        }


        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public List<UserVO> search(String keyword) {
        //1.构建查询条件:昵称模糊 或 手机号精确
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getNickname, keyword)
                        .or()
                        .eq(User::getPhone, keyword));
        //2.实体列表转VO列表
        return users.stream().map(UserVO::from).toList();
    }
}
