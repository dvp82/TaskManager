package com.dvp.taskmanager.config;

import com.dvp.taskmanager.security.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtTokenFilter bearerFilter;
    private final RestAuthenticationEntryPoint unauthenticatedHandler;
    private final RestAccessDeniedHandler deniedHandler;

    public SecurityConfiguration(JwtTokenFilter bearerFilter,
                                 RestAuthenticationEntryPoint unauthenticatedHandler,
                                 RestAccessDeniedHandler deniedHandler) {
        this.bearerFilter = bearerFilter;
        this.unauthenticatedHandler = unauthenticatedHandler;
        this.deniedHandler = deniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .headers().frameOptions().sameOrigin()
                .and()
                .authorizeRequests(auth -> auth
                    .antMatchers("/api/auth/**", "/h2-console/**").permitAll()
                    .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .antMatchers("/api/tasks/**").authenticated()
                    .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthenticatedHandler)
                        .accessDeniedHandler(deniedHandler)
                )
                .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
