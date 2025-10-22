package com.spotify.dashboard.service;

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

    // injects dependencies, and we input the restTemplate for all reqs, and the base url (https://api.spotify.com/v1)
    public SpotifyApiService(RestTemplate restTemplate, @Value("${spotify.api.base-url}") String spotifyApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.spotifyApiBaseUrl = spotifyApiBaseUrl;
    }

    private HttpHeaders createHeaders(String accessToken) {
    // attach user's OAuth access token to every request (server will validate scopes)
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    // Used for requests with JSON bodies (POST/PUT); harmless on GET.
    headers.set("Content-Type", "application/json");
    return headers;
}

    // Template that we use throughout the queries/tasks specifically for get requests
    @SuppressWarnings("unchecked")
    private Map<String, Object> makeGetRequest(String endpoint, String accessToken) {
        // HttpEntity is a Spring container for an HTTP req or response, to hold a body and headers together
        // sort of like an envelope that wraps the headers and optional body so RestTemplate knows what to send
        try {
            HttpEntity<String> entity = new HttpEntity<>(createHeaders(accessToken)); // no body, so empty-body req with headers
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange( 
                spotifyApiBaseUrl + endpoint,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            // spotifyApiBaseUrl + endpoint sets up https://api.spotify.com/v1/me/top/tracks?, 
            // from there, we call the get request (HttpMethod.GET), headers (entity), 
            // tell the RestTemplate to deserialize the JSON response to a map ((Class<Map<String, Object>>) (Class<?>) Map.class)
            return response.getBody(); // from there, it's now in Map<String, Object> format instead of in JSON 
        } catch (Exception e) {
            throw new RuntimeException("API request failed: " + e.getMessage()); // catches HTTP errors, network, deserialization errors, etc 
        }
    }

    // Template that we use throughout the API calls specifically for post requests
    // Same comments as before basically
    // Only difference is that makePostRequest a payload (body) and uses HttpMethod.POST
    @SuppressWarnings("unchecked")
    private Map<String, Object> makePostRequest(String endpoint, String accessToken, Object body) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(body, createHeaders(accessToken));
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                spotifyApiBaseUrl + endpoint,
                HttpMethod.POST,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("API request failed: " + e.getMessage());
        }
    }

    // Reference the Spotify Web API for the diff endpoints, paths, params, required scopes, and JSON schemas
    // they're basically provided to us yayayay
    // below are the API call methods that build the speciifc Spotify endpoint/path (and query params if needed),
    // then returns the response parsed from JSON into Map<String, Object> for easy accessibility

    @Cacheable(value = "user", key = "#accessToken.hashCode()")
    public Map<String, Object> getCurrentUser(String accessToken) {
        return makeGetRequest("/me", accessToken);
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

    @Cacheable(value = "artists", key = "#accessToken.hashCode() + '_' + #artistIds.hashCode()")
    public Map<String, Object> getArtists(String accessToken, java.util.List<String> artistIds) {
        String ids = String.join(",", artistIds);
        String endpoint = "/artists?ids=" + ids;
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