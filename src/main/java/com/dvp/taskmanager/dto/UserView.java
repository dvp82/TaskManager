package com.dvp.taskmanager.dto;

import com.dvp.taskmanager.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Профиль пользователя")
public class UserView {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "john")
    private String username;

    @Schema(example = "john@example.com")
    private String email;

    @Schema(example = "USER")
    private Role role;
}