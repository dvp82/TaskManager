package com.dvp.taskmanager.config;

import com.dvp.taskmanager.model.Role;
import com.dvp.taskmanager.model.Task;
import com.dvp.taskmanager.model.TaskPriority;
import com.dvp.taskmanager.model.TaskStatus;
import com.dvp.taskmanager.model.User;
import com.dvp.taskmanager.repository.TaskRepository;
import com.dvp.taskmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository,
                               TaskRepository taskRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        User user = userRepository.save(User.builder()
                .username("user")
                .email("user@example.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        taskRepository.save(Task.builder()
                .title("Обновить проект")
                .description("Обновить зависимости")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .author(admin)
                .assignee(user)
                .build());

        taskRepository.save(Task.builder()
                .title("Написать отчёт")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .author(admin)
                .assignee(null)
                .build());

        taskRepository.save(Task.builder()
                .title("Проверить код")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.MEDIUM)
                .author(user)
                .assignee(admin)
                .build());

        log.info("Saved users: admin/admin123 (ADMIN), user/user123 (USER)");
    }
}