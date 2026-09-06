package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.dto.DeleteMessageDTO;
import com.gym.easychatjava.service.MessageService;
import com.gym.easychatjava.vo.MessageVO;
import com.gym.easychatjava.websocket.ChatWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天接口(拉历史消息等,发消息走 WebSocket)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final ChatWebSocketHandler chatWebSocketHandler;

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

    /** 撤回消息:校验通过后标记已撤回,并实时通知接收方 */
    @PostMapping("/recall")
    public Result<Void> recall(@RequestParam Long messageId){
        MessageVO recallVo=messageService.recall(messageId);
        chatWebSocketHandler.pushRecall(recallVo.getToId(),recallVo);
        return Result.success();
    }

    /**删除消息*/
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody DeleteMessageDTO dto){
        messageService.deleteMessages(dto.getMessageIds());
        return Result.success();
    }
}
