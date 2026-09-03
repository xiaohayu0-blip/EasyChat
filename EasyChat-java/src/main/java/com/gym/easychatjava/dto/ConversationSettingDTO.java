package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversationSettingDTO {

    @NotNull(message = "对方ID不能为空")
    private Long peerId;

    @NotNull(message = "开关值不能为空")
    private Boolean enabled;
}
