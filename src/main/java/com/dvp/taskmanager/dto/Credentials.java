package com.dvp.taskmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Учётные данные для входа")
public class Credentials {

    @NotBlank(message = "Username is required")
    @Schema(description = "Логин", example = "john")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Пароль", example = "secret123")
    private String password;
}