package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.dto.CommentDTO;
import com.gym.easychatjava.dto.PublishMomentDTO;
import com.gym.easychatjava.service.MomentService;
import com.gym.easychatjava.vo.MomentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moment")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody PublishMomentDTO dto){
        momentService.publish(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMoment(@PathVariable Long id){
        momentService.deleteMoment(id);
        return Result.success();
    }

    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id){
        momentService.like(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id){
        momentService.unlike(id);
        return Result.success();
    }

    @PostMapping("/comment")
    public Result<Void> comment(@Valid @RequestBody CommentDTO dto){
        momentService.comment(dto);
        return Result.success();
    }

    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id){
        momentService.deleteComment(id);
        return Result.success();
    }

    @GetMapping("/timeline")
    public Result<List<MomentVO>> timeline(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size){
        return Result.success(momentService.listTimeline(page, size));
    }

    @GetMapping("/{id}")
    public Result<MomentVO> detail(@PathVariable Long id){
        return Result.success(momentService.getMomentDetail(id));
    }

    @GetMapping("/user/{userId}")
    public Result<List<MomentVO>> userMoments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size){
        return Result.success(momentService.listUserMoments(userId, page, size));
    }
}
