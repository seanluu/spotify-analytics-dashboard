package com.spotify.dashboard.service;

import com.spotify.dashboard.util.SpotifyAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TokenRefreshService {

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final SpotifyAuthUtil spotifyAuthUtil;

    @Value("${spotify.client-id}")
    private String clientId;
    
    @Value("${spotify.client-secret}")
    private String clientSecret;

    public TokenRefreshService(RestTemplate restTemplate, UserService userService, SpotifyAuthUtil spotifyAuthUtil) {
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.spotifyAuthUtil = spotifyAuthUtil;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        HttpHeaders headers = spotifyAuthUtil.createBasicAuthHeaders(clientId, clientSecret);
        String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            SpotifyAuthUtil.SPOTIFY_TOKEN_URL,
            HttpMethod.POST,
            entity,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        return response.getBody();
    }

    public Map<String, Object> refreshAccessTokenForUser(String spotifyId) {
        String refreshToken = userService.getRefreshToken(spotifyId);
        if (refreshToken == null) {
            throw new RuntimeException("No refresh token found for user");
        }
        
        Map<String, Object> tokenResponse = refreshAccessToken(refreshToken);
        
        if (tokenResponse.containsKey("refresh_token")) {
            userService.updateRefreshToken(spotifyId, (String) tokenResponse.get("refresh_token"));
        }
        
        return tokenResponse;
    }
}