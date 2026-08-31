package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRemarkDTO {

    /** 好友ID */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;

    /** 备注名,可为空(空表示清除备注) */
    private String remark;
}
