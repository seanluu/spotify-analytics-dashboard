package com.spotify.dashboard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class SpotifyTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (path.equals("/api/v1/spotify/auth/callback") || path.equals("/api/spotify/auth/callback")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (path.startsWith("/api/")) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Authorization header required\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

