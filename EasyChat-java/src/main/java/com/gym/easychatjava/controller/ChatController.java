package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天接口(拉历史消息等,发消息走 WebSocket)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;

    /** 拉取与某好友的历史消息(倒序分页) */
    @GetMapping("/history")
    public Result<List<MessageVO>> listHistory(
            @RequestParam Long friendId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size){
        return Result.success(messageService.listHistory(friendId,page,size));
    }

    /** 查询与某好友的未读消息数 */
    @GetMapping("/unread")
    public Result<Integer> getUnreadCount(@RequestParam Long friendId) {
        return Result.success(messageService.getUnreadCount(friendId));
    }
}
