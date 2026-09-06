package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.dto.FriendRequestDTO;
import com.gym.easychatjava.dto.HandleRequestDTO;
import com.gym.easychatjava.dto.UpdateRemarkDTO;
import com.gym.easychatjava.service.FriendService;
import com.gym.easychatjava.vo.FriendRequestVO;
import com.gym.easychatjava.vo.FriendVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 好友接口(申请/同意/列表/删除)
 */
@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 发送好友申请
     */
    @PostMapping("/request")
    public Result<Void> sendRequest(@Valid @RequestBody FriendRequestDTO dto) {
        friendService.sendRequest(dto);
        return Result.success();
    }

    @GetMapping("/request/received")
    public Result<List<FriendRequestVO>> listReceivedRequests(){
        return Result.success(friendService.listReceivedRequests());
    }

    @PutMapping("/request/{id}")
    public Result<Void> handleRequest(@PathVariable Long id, @Valid @RequestBody HandleRequestDTO dto){
        friendService.handleRequest(id,dto);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<FriendVO>> listFriends(){
        return Result.success(friendService.listFriends());
    }

    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@PathVariable Long friendId){
        friendService.deleteFriend(friendId);
        return Result.success();
    }

    @PutMapping("/remark")
    public Result<Void> updateRemark(@Valid @RequestBody UpdateRemarkDTO dto){
        friendService.updateRemark(dto);
        return Result.success();
    }

    /**拉黑好友*/
    @PutMapping("/block/{friendId}")
    public Result<Void> blockFriend(@PathVariable Long friendId){
        friendService.blockFriend(friendId);
        return Result.success();
    }

    /**移出黑名单*/
    @PutMapping("/unblock/{friendId}")
    public Result<Void> unblockFriend(@PathVariable Long friendId){
        friendService.unblockFriend(friendId);
        return Result.success();
    }

    /**获取未读好友申请数*/
    @GetMapping("/request/unread")
    public Result<Integer> getUnreadRequestCount(){
        return Result.success(friendService.getUnreadRequestCount());
    }
}
