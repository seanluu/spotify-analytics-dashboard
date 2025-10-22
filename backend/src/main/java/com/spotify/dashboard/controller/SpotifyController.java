package com.spotify.dashboard.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.spotify.dashboard.service.SpotifyApiService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController // tells Spring this handles the HTTP requests and returns JSON
@RequestMapping("/api/spotify") // ensures all endpoints start with /api/spotify

public class SpotifyController {

    private final RestTemplate restTemplate;
    
    private static final String TIME_RANGE_PATTERN = "^(short_term|medium_term|long_term)$";
    private static final String TIME_RANGE_ERROR = "Invalid time range";
    private static final String DEFAULT_TIME_RANGE = "medium_term";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";

    @Value("${spotify.client-id}")
    private String clientId;
    
    @Value("${spotify.client-secret}")
    private String clientSecret;
    
    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final SpotifyApiService spotifyApiService;

    public SpotifyController(SpotifyApiService spotifyApiService, RestTemplate restTemplate) {
        this.spotifyApiService = spotifyApiService;
        this.restTemplate = restTemplate;
    }

    // if it's in "Bearer hi123" format, it'll only give us the token (hi123)
    private String extractAccessToken(String authHeader) {
        return authHeader.replace("Bearer ", "");
    }

    // Standardizes error responses across all endpoints
    private ResponseEntity<Map<String, Object>> handleError(Exception e) {
        return ResponseEntity.badRequest() // for 400 error status
            .body(Map.of("error", e.getMessage())); // returns JSON in format of {"error" : "message"}
    }

    @GetMapping("/me")
    private ResponseEntity<Map<String, Object>> getCurrentUser( 
        @RequestHeader("Authorization") String authHeader) {
            try {
                String accessToken = extractAccessToken(authHeader); // Only gets the specific token "hi123"
                Map<String, Object> result = spotifyApiService.getCurrentUser(accessToken); // then call Spotify API w/ that token
                return ResponseEntity.ok(result); // HTTP 200 success + user data JSON
            } catch (Exception e) {
                return handleError(e); // otherwise it's http 400 error + error JSON
            }   
        }

    @GetMapping("/top/tracks")
    private ResponseEntity<Map<String, Object>> getTopTracks( 
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = DEFAULT_TIME_RANGE)
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR) // Validates time_range matches regex, returns 400 if invalid
        String time_range, 
        @RequestParam(defaultValue = "50") // Default limit when parameter not provided
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 50, message = "Limit can't exceed 50") // Spotify only provides up to top 50 tracks/artists
        int limit) {
            try {
                String accessToken = extractAccessToken(authHeader);
                Map<String, Object> result = spotifyApiService.getTopTracks(accessToken, time_range, limit);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return handleError(e);
            }   
        }
    
    // Follow same format as before just for artists
    @GetMapping("/top/artists")
    private ResponseEntity<Map<String, Object>> getTopArtists( 
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = DEFAULT_TIME_RANGE)
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String time_range, 
        @RequestParam(defaultValue = "50") 
        @Min(value = 1, message = "Limit must be at least 1") 
        @Max(value = 50, message = "Limit can't exceed 50") 
        int limit) {
            try {
                String accessToken = extractAccessToken(authHeader);
                Map<String, Object> result = spotifyApiService.getTopArtists(accessToken, time_range, limit);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return handleError(e);
            }   
        }

    // Follow same format as before just for artists
    @PostMapping("/playlists/generate")
    private ResponseEntity<Map<String, Object>> generatePlaylist( 
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody PlaylistGenerationRequest request) {
            try {
                String accessToken = extractAccessToken(authHeader);

                // Getting the current user's info first
                Map<String, Object> user = spotifyApiService.getCurrentUser(accessToken);
                String userId = (String) user.get("id");

                // Getting user's top tracks based on timeframe chosen
                Map<String, Object> tracksResponse = spotifyApiService.getTopTracks(accessToken, request.time_range, 50);
                @SuppressWarnings("unchecked")
                var tracks = (java.util.List<Map<String, Object>>) tracksResponse.get("items");

                // Edge case if we don't find any tracks lol
                if (tracks == null || tracks.isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                    "message", "No tracks found for the selected time period",
                    "name", request.name
                    ));
                }

                // Create playlist 
                Map<String, Object> playlistData = Map.of(
                    "name", request.name, 
                    "description", request.description, 
                    "public", request.public_playlist 
                );

                Map<String, Object> playlist = spotifyApiService.createPlaylist(accessToken, userId, playlistData);
                String playlistId = (String) playlist.get("id");

                // Extract track URIs
                var tracksUri = tracks.stream()
                    .map(track -> (String) track.get("uri"))
                    .collect(java.util.stream.Collectors.toList());

                // Adding tracks to playlist
                Map<String, Object> tracksData = Map.of("uris", tracksUri);
                spotifyApiService.addTracksToPlaylist(accessToken, playlistId, tracksData);

                // Success response w/ playlist info when completed
                return ResponseEntity.ok(Map.of( 
                "id", playlistId, 
                "name", playlist.get("name"), 
                "description", playlist.get("description"), 
                "tracks_added", tracksUri.size(), 
                "external_urls", playlist.get("external_urls") // Spotify url
                ));

            } catch (Exception e) {
                return handleError(e);
            }   

        }

    @GetMapping("/analytics/genres") // Use post instead of get
    private ResponseEntity<Map<String, Object>> getGenreAnalytics( 
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = DEFAULT_TIME_RANGE)
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String time_range) {
            try {
                String accessToken = extractAccessToken(authHeader);

                // Grab user's top artists
                Map<String, Object> artistsResponse = spotifyApiService.getTopArtists(accessToken, time_range, 50);
                @SuppressWarnings("unchecked")
                var artists = (java.util.List<Map<String, Object>>) artistsResponse.get("items");

                if (artists == null || artists.isEmpty()) { // edge case again if we don't see any artists
                    return ResponseEntity.ok(Map.of("items", java.util.Collections.emptyList()));
                }

                // Grab all genres from all the artists the user listens to
                var genreCount = new java.util.HashMap<String, Integer>();
                artists.forEach(artist -> {
                    @SuppressWarnings("unchecked")
                    var genres = (java.util.List<String>) artist.get("genres");
                    if (genres != null) {
                        genres.forEach(genre -> genreCount.merge(genre, 1, Integer::sum)); // Count every time a genre occurs
                    }
                });

                // Convert them to a sorted list w/ percentages (for the pie chart)
                var genreList = genreCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Sort in descending order (count)
                    .limit(10) // Their top 10 genres they listen to
                    .map(entry -> {
                        int count = entry.getValue();
                        double percentage = (count * 100.0) / artists.size();
                        return Map.of(
                            "name", entry.getKey(), 
                            "count", count, 
                            "percentage", Math.round(percentage * 100.0) / 100.0 // Round percentage to 2 decimals
                        );
                    }) 
                    .collect(java.util.stream.Collectors.toList());

                return ResponseEntity.ok(Map.of("items", genreList));

            } catch (Exception e) {
                return handleError(e);
            }   
        }

    @PostMapping("/auth/callback") // Use post instead of get 
    public ResponseEntity<Map<String, Object>> handleAuthCallback(
        @RequestBody Map<String, String> request) { // JSON body instead of using query parameters
            try {
                String code = request.get("code"); // Extracting the auth code from JSON format
                if (code == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "No auth code provided"));
                }

                // Encode client creds for basic auth
                String credentials = clientId + ":" + clientSecret;
                String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

                // Headers for token exchange
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/x-www-form-urlencoded"); // form data
                headers.set("Authorization", "Basic " + encodedCredentials); // basic auth

                // Build form data body
                String body = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + redirectUri;
                HttpEntity<String> entity = new HttpEntity<>(body, headers);

                // Exchange the code for tokens
                @SuppressWarnings("unchecked")
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    SPOTIFY_TOKEN_URL, 
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
                );

                return ResponseEntity.ok(response.getBody()); // Returns: {access_token, refresh_token, expires_in}
            } catch (Exception e) {
                return handleError(e);
            }
        }
    

    public static class PlaylistGenerationRequest {
        public String template;  // Optional field
        
        @NotBlank(message = "Time range is required")
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)  // Validate time range
        public String time_range;
        
        @NotBlank(message = "Playlist name is required")
        public String name;
        
        public String description;  // Optional
        public boolean public_playlist;  // Optional, defaults to false

        // Getters and setters for each field
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
        public String getTime_range() { return time_range; }
        public void setTime_range(String time_range) { this.time_range = time_range; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isPublic_playlist() { return public_playlist; }
        public void setPublic_playlist(boolean public_playlist) { this.public_playlist = public_playlist; }
    }
}
