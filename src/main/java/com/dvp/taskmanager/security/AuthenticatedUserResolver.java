package com.dvp.taskmanager.security;

import com.dvp.taskmanager.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public AuthenticatedUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long resolveId() {
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal == null || principal.getName() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Unknown principal " + principal.getName()))
                .getId();
    }
}