package com.gym.easychatjava.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordDTO {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min=6,max=20)
    private String newPassword;
}
