package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.dto.ConversationSettingDTO;
import com.gym.easychatjava.service.ConversationService;
import com.gym.easychatjava.vo.ConversationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/list")
    public Result<List<ConversationVO>> list(){
        return Result.success(conversationService.listConversations());
    }

    @PutMapping("/pin")
    public Result<Void> pin(@Valid @RequestBody ConversationSettingDTO dto) {
        conversationService.setPinned(dto);
        return Result.success();
    }

    @PutMapping("/mute")
    public Result<Void> mute(@Valid @RequestBody ConversationSettingDTO dto) {
        conversationService.setMuted(dto);
        return Result.success();
    }
}
