package com.dvp.taskmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Выданный JWT-токен")
public class TokenPayload {

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(example = "Bearer")
    private String type;

    @Schema(example = "john")
    private String username;

    @Schema(example = "USER")
    private String role;

    @Schema(description = "Срок жизни в миллисекундах", example = "86400000")
    private long expiresIn;

    public static TokenPayload of(String token, String username, String role, long expiresIn) {
        return new TokenPayload(token, "Bearer", username, role, expiresIn);
    }
}