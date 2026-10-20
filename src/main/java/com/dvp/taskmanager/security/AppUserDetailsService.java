package com.dvp.taskmanager.security;

import com.dvp.taskmanager.model.User;
import com.dvp.taskmanager.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user for " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(),
                java.util.List.<org.springframework.security.core.GrantedAuthority>of(
                        () -> "ROLE_" + user.getRole().name()));
    }
}