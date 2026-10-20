package com.dvp.taskmanager.service;

import com.dvp.taskmanager.dto.TokenPayload;
import com.dvp.taskmanager.dto.Credentials;
import com.dvp.taskmanager.dto.RegisterCommand;
import com.dvp.taskmanager.dto.UserView;
import com.dvp.taskmanager.exception.BadRequestException;
import com.dvp.taskmanager.model.Role;
import com.dvp.taskmanager.model.User;
import com.dvp.taskmanager.repository.UserRepository;
import com.dvp.taskmanager.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserView register(RegisterCommand command) {
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User fresh = User.builder()
            .username(command.getUsername())
            .email(command.getEmail())
            .password(passwordEncoder.encode(command.getPassword()))
            .role(Role.USER)
            .build();

        return toView(userRepository.save(fresh));
    }

    @Transactional(readOnly = true)
    public TokenPayload login(Credentials credentials) {
        User user = userRepository.findByUsername(credentials.getUsername())
            .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }
        String token = tokenService.issue(user.getUsername(), user.getRole().name());
        long expiresIn = tokenService.lifetimeMillis();
        return TokenPayload.of(
            token,
            user.getUsername(),
            user.getRole().name(),
            expiresIn);
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}