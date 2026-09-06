package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeleteMessageDTO {

    @NotEmpty(message="消息ID列表不能为空")
    private List<Long> messageIds;
}
