package com.dvp.taskmanager.security;

import com.dvp.taskmanager.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long lifetimeMillis;

    public TokenService(JwtProperties properties) {
        this.signingKey = createKey(properties.secret());
        this.lifetimeMillis = properties.expirationMs();
    }

    public String issue(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + lifetimeMillis))
            .signWith(signingKey)
            .compact();
    }

    public String usernameOf(String rawToken) {
        return read(rawToken, Claims::getSubject);
    }

    public long lifetimeMillis() {
        return lifetimeMillis;
    }

    private <T> T read(String rawToken, Function<Claims, T> extractor) {
        Claims details = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(rawToken)
            .getPayload();
        return extractor.apply(details);
    }

    private SecretKey createKey(String secret) {
        byte[] raw;
        try {
            raw = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            raw = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(raw);
    }
}