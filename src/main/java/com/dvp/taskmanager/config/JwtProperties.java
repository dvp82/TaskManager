package com.dvp.taskmanager.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 86_400_000L;

    public String secret() {
        return secret;
    }

    public long expirationMs() {
        return expirationMs;
    }

}