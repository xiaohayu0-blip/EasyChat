package com.gym.easychatjava.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDTO {

    @Size(max=50)
    private String nickname;

    @Size(max=255)
    private String avatar;

    @Min(0)
    @Max(2)
    private Integer gender;

    @Size(max=200)
    private String signature;

}
