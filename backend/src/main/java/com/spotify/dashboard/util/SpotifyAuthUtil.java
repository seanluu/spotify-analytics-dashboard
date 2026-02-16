package com.spotify.dashboard.util;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class SpotifyAuthUtil {

    public static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";

    public HttpHeaders createBasicAuthHeaders(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = java.util.Base64.getEncoder()
            .encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        headers.set("Authorization", "Basic " + encodedCredentials);
        return headers;
    }

    public HttpHeaders createBearerAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}

