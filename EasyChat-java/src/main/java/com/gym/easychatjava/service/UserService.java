package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.LoginDTO;
import com.gym.easychatjava.dto.RegisterDTO;
import com.gym.easychatjava.dto.UpdatePasswordDTO;
import com.gym.easychatjava.dto.UpdateUserDTO;
import com.gym.easychatjava.vo.LoginVO;
import com.gym.easychatjava.vo.UserVO;

import java.util.List;

public interface UserService {

    void register(RegisterDTO registerDTO);
    LoginVO login(LoginDTO loginDTO);

    /** 查看当前登录用户资料 */
    UserVO getMyInfo();

    UserVO updateMyInfo(UpdateUserDTO dto);

    void updatePassword(UpdatePasswordDTO dto);

    List<UserVO> search(String keyword);
}
