package com.dvp.taskmanager.controller;

import com.dvp.taskmanager.dto.Credentials;
import com.dvp.taskmanager.dto.RegisterCommand;
import com.dvp.taskmanager.dto.TokenPayload;
import com.dvp.taskmanager.dto.UserView;
import com.dvp.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Регистрация и вход в систему")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация нового пользователя")
    public UserView register(@Valid @RequestBody RegisterCommand command) {
        return authService.register(command);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход и получение JWT-токена")
    public TokenPayload login(@Valid @RequestBody Credentials credentials) {
        return authService.login(credentials);
    }
}