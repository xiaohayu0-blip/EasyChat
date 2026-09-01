package com.gym.easychatjava.controller;

import com.gym.easychatjava.common.Result;
import com.gym.easychatjava.dto.PublishMomentDTO;
import com.gym.easychatjava.service.MomentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
