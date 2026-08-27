package com.gym.easychatjava.service;

import com.gym.easychatjava.dto.LoginDTO;
import com.gym.easychatjava.dto.RegisterDTO;
import com.gym.easychatjava.vo.LoginVO;

public interface UserService {
    void register(RegisterDTO registerDTO);
    LoginVO login(LoginDTO loginDTO);
}
