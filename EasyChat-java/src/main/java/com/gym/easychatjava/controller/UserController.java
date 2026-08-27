package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.dto.UpdatePasswordDTO;
import com.gym.easychatjava.dto.UpdateUserDTO;
import com.gym.easychatjava.service.UserService;
import com.gym.easychatjava.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户信息接口(查/改/搜索)
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查看当前登录用户资料
     */
    @GetMapping("/me")
    public Result<UserVO> getMyInfo() {
        return Result.success(userService.getMyInfo());
    }

    @PutMapping("/me")
    public Result<UserVO> updateMyInfo(@Valid @RequestBody UpdateUserDTO dto) {
        return Result.success(userService.updateMyInfo(dto));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(dto);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<List<UserVO>> search(@RequestParam String keyword) {
        return Result.success(userService.search(keyword));
    }
}
