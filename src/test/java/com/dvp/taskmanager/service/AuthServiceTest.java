package com.dvp.taskmanager.service;

import com.dvp.taskmanager.dto.Credentials;
import com.dvp.taskmanager.dto.RegisterCommand;
import com.dvp.taskmanager.dto.TokenPayload;
import com.dvp.taskmanager.dto.UserView;
import com.dvp.taskmanager.exception.BadRequestException;
import com.dvp.taskmanager.model.Role;
import com.dvp.taskmanager.model.User;
import com.dvp.taskmanager.repository.UserRepository;
import com.dvp.taskmanager.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private AuthService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AuthService(userRepository, passwordEncoder, tokenService);
    }

    @Test
    void register_shouldHashPasswordAndGrantUserRole() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserView view = underTest.register(new RegisterCommand("john", "john@example.com", "secret123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(view.getUsername()).isEqualTo("john");
        assertThat(saved.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_shouldRejectTakenUsername() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> underTest.register(new RegisterCommand("john", "john@example.com", "123456")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnTokenForKnownUser() {
        User user = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded")).thenReturn(true);
        when(tokenService.issue("john", "USER")).thenReturn("jwt-token");
        when(tokenService.lifetimeMillis()).thenReturn(86_400_000L);

        TokenPayload payload = underTest.login(new Credentials("john", "secret123"));

        assertThat(payload.getToken()).isEqualTo("jwt-token");
        assertThat(payload.getUsername()).isEqualTo("john");
        assertThat(payload.getRole()).isEqualTo("USER");
        assertThat(payload.getType()).isEqualTo("Bearer");
        assertThat(payload.getExpiresIn()).isEqualTo(86_400_000L);
    }

    @Test
    void login_shouldRejectWrongPassword() {
        User user = User.builder()
                .id(1L).username("john").email("a@b.c").password("encoded").role(Role.USER)
                .build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> underTest.login(new Credentials("john", "wrong-password")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid username or password");
    }
}