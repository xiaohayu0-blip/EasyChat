package com.gym.easychatjava.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PublishMomentDTO {

    private String content;

    @Size(max=9,message="最多上传九张图片")
    private List<String> images;
}
