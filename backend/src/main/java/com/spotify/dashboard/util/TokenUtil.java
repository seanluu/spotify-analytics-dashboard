package com.spotify.dashboard.util;

import org.springframework.stereotype.Component;

@Component
public class TokenUtil {

    public String extractAccessToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be empty");
        }
        return token;
    }
}

