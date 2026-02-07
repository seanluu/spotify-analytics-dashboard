package com.spotify.dashboard.service;

import com.spotify.dashboard.util.SpotifyAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SpotifyApiService {

    private final RestTemplate restTemplate;
    private final String spotifyApiBaseUrl;
    private final SpotifyAuthUtil spotifyAuthUtil;

    public SpotifyApiService(RestTemplate restTemplate, 
                           @Value("${spotify.api.base-url}") String spotifyApiBaseUrl,
                           SpotifyAuthUtil spotifyAuthUtil) {
        this.restTemplate = restTemplate;
        this.spotifyApiBaseUrl = spotifyApiBaseUrl;
        this.spotifyAuthUtil = spotifyAuthUtil;
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = spotifyAuthUtil.createBearerAuthHeaders(accessToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> makeGetRequest(String endpoint, String accessToken) {
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(accessToken));
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange( 
            spotifyApiBaseUrl + endpoint,
            HttpMethod.GET,
            entity,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> makePostRequest(String endpoint, String accessToken, Object body) {
        HttpEntity<Object> entity = new HttpEntity<>(body, createHeaders(accessToken));
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            spotifyApiBaseUrl + endpoint,
            HttpMethod.POST,
            entity,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        return response.getBody();
    }

    @Cacheable(value = "user", key = "#accessToken.hashCode()")
    public Map<String, Object> getCurrentUser(String accessToken) {
        return makeGetRequest("/me", accessToken);
    }

    public String getUserId(String accessToken) {
        Map<String, Object> user = getCurrentUser(accessToken);
        return (String) user.get("id");
    }

    @Cacheable(value = "topTracks", key = "#accessToken.hashCode() + '_' + #timeRange + '_' + #limit")
    public Map<String, Object> getTopTracks(String accessToken, String timeRange, int limit) {
        String endpoint = "/me/top/tracks?time_range=" + timeRange + "&limit=" + limit;
        return makeGetRequest(endpoint, accessToken);
    }

    @Cacheable(value = "topArtists", key = "#accessToken.hashCode() + '_' + #timeRange + '_' + #limit")
    public Map<String, Object> getTopArtists(String accessToken, String timeRange, int limit) {
        String endpoint = "/me/top/artists?time_range=" + timeRange + "&limit=" + limit;
        return makeGetRequest(endpoint, accessToken);
    }

    public Map<String, Object> createPlaylist(String accessToken, String userId, Map<String, Object> playlistData) {
        String endpoint = "/users/" + userId + "/playlists";
        return makePostRequest(endpoint, accessToken, playlistData);
    }

    public Map<String, Object> addTracksToPlaylist(String accessToken, String playlistId, Map<String, Object> tracksData) {
        String endpoint = "/playlists/" + playlistId + "/tracks";
        return makePostRequest(endpoint, accessToken, tracksData);
    }
}