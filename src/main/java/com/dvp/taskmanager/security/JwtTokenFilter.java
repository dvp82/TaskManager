package com.dvp.taskmanager.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AppUserDetailsService userLookup;

    public JwtTokenFilter(TokenService tokenService, AppUserDetailsService userLookup) {
        this.tokenService = tokenService;
        this.userLookup = userLookup;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String rawToken = header.substring(7);
        try {
            String username = tokenService.usernameOf(rawToken);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails principal = userLookup.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken grant = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                grant.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(grant);
            }
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}