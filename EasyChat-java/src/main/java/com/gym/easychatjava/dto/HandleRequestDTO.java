package com.gym.easychatjava.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleRequestDTO {
    @NotNull
    @Min(1)
    @Max(2)
    private Integer status;   // 1同意 2拒绝
}
