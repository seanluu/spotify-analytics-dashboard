package com.spotify.dashboard.controller;

import java.util.HashMap;
import java.util.List;
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
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.dashboard.service.SpotifyApiService;
import com.spotify.dashboard.service.UserService;
import com.spotify.dashboard.service.AudioFeaturesService;
import com.spotify.dashboard.util.TokenUtil;
import com.spotify.dashboard.util.SpotifyAuthUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);
    private final RestTemplate restTemplate;
    
    private static final String TIME_RANGE_PATTERN = "^(short_term|medium_term|long_term)$";
    private static final String TIME_RANGE_ERROR = "Invalid time range";
    private static final String DEFAULT_TIME_RANGE = "medium_term";
    private static final int DEFAULT_TOP_ITEMS_LIMIT = 50;

    @Value("${spotify.client-id}")
    private String clientId;
    
    @Value("${spotify.client-secret}")
    private String clientSecret;
    
    @Value("${spotify.redirect-uri}")
    private String redirectUri;
    
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private final SpotifyApiService spotifyApiService;
    private final UserService userService;
    private final com.spotify.dashboard.service.ListeningHistoryService listeningHistoryService;
    private final AudioFeaturesService audioFeaturesService;
    private final TokenUtil tokenUtil;
    private final SpotifyAuthUtil spotifyAuthUtil;

    public SpotifyController(SpotifyApiService spotifyApiService, 
                           UserService userService,
                           com.spotify.dashboard.service.ListeningHistoryService listeningHistoryService,
                           AudioFeaturesService audioFeaturesService,
                           RestTemplate restTemplate,
                           TokenUtil tokenUtil,
                           SpotifyAuthUtil spotifyAuthUtil) {
        this.spotifyApiService = spotifyApiService;
        this.userService = userService;
        this.listeningHistoryService = listeningHistoryService;
        this.audioFeaturesService = audioFeaturesService;
        this.restTemplate = restTemplate;
        this.tokenUtil = tokenUtil;
        this.spotifyAuthUtil = spotifyAuthUtil;
    }
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser( 
        @RequestHeader("Authorization") String authHeader) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);
        Map<String, Object> result = spotifyApiService.getCurrentUser(accessToken);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/top/tracks")
    public ResponseEntity<Map<String, Object>> getTopTracks( 
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = DEFAULT_TIME_RANGE)
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String time_range, 
        @RequestParam(defaultValue = "50")
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 50, message = "Limit can't exceed 50")
        int limit) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);
        Map<String, Object> result = spotifyApiService.getTopTracks(accessToken, time_range, limit);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/top/artists")
    public ResponseEntity<Map<String, Object>> getTopArtists( 
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = DEFAULT_TIME_RANGE)
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String time_range, 
        @RequestParam(defaultValue = "50") 
        @Min(value = 1, message = "Limit must be at least 1") 
        @Max(value = 50, message = "Limit can't exceed 50") 
        int limit) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);
        Map<String, Object> result = spotifyApiService.getTopArtists(accessToken, time_range, limit);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/playlists/generate")
    public ResponseEntity<Map<String, Object>> generatePlaylist( 
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody PlaylistGenerationRequest request) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);
        String userId = spotifyApiService.getUserId(accessToken);

        Map<String, Object> tracksResponse = spotifyApiService.getTopTracks(accessToken, request.time_range, DEFAULT_TOP_ITEMS_LIMIT);
        @SuppressWarnings("unchecked")
        var tracks = (List<Map<String, Object>>) tracksResponse.get("items");

        if (tracks == null || tracks.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No tracks found for the selected time period",
                "name", request.name
            ));
        }

        Map<String, Object> playlistData = Map.of(
            "name", request.name, 
            "description", request.description, 
            "public", request.public_playlist 
        );

        Map<String, Object> playlist = spotifyApiService.createPlaylist(accessToken, userId, playlistData);
        String playlistId = (String) playlist.get("id");

        var tracksUri = tracks.stream()
            .map(track -> (String) track.get("uri"))
            .toList();

        Map<String, Object> tracksData = Map.of("uris", tracksUri);
        spotifyApiService.addTracksToPlaylist(accessToken, playlistId, tracksData);

        return ResponseEntity.ok(Map.of( 
            "id", playlistId, 
            "name", playlist.get("name"), 
            "description", playlist.get("description"), 
            "tracks_added", tracksUri.size(), 
            "external_urls", playlist.get("external_urls")
        ));
    }

    @GetMapping("/analytics/genres")
    public ResponseEntity<Map<String, Object>> getGenreAnalytics( 
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = DEFAULT_TIME_RANGE)
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String time_range) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);

        Map<String, Object> artistsResponse = spotifyApiService.getTopArtists(accessToken, time_range, DEFAULT_TOP_ITEMS_LIMIT);
        @SuppressWarnings("unchecked")
        var artists = (List<Map<String, Object>>) artistsResponse.get("items");

        if (artists == null || artists.isEmpty()) {
            return ResponseEntity.ok(Map.of("items", List.of()));
        }

        var genreCount = new HashMap<String, Integer>();
        artists.forEach(artist -> {
            @SuppressWarnings("unchecked")
            var genres = (List<String>) artist.get("genres");
            if (genres != null) {
                genres.forEach(genre -> genreCount.merge(genre, 1, Integer::sum));
            }
        });

        var genreList = genreCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(entry -> {
                int count = entry.getValue();
                double percentage = (count * 100.0) / artists.size();
                return Map.of(
                    "name", entry.getKey(), 
                    "count", count, 
                    "percentage", Math.round(percentage * 100.0) / 100.0
                );
            }) 
            .toList();

        return ResponseEntity.ok(Map.of("items", genreList));
    }

    @PostMapping("/listening-history/poll")
    public ResponseEntity<Map<String, Object>> pollListeningHistory(
            @RequestHeader("Authorization") String authHeader) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);
        String userId = spotifyApiService.getUserId(accessToken);
        listeningHistoryService.pollRecentlyPlayed(userId, accessToken);
        return ResponseEntity.ok(Map.of("message", "Listening history updated"));
    }

    @GetMapping("/auth/callback")
    public RedirectView handleAuthCallbackGet(
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "error", required = false) String error,
        HttpServletResponse httpResponse) {
        
        if (error != null) {
            logger.error("Spotify authorization error: {}", error);
            return new RedirectView(frontendUrl + "/callback?error=" + error);
        }

        if (code == null || code.isEmpty()) {
            logger.error("No authorization code provided in callback");
            return new RedirectView(frontendUrl + "/callback?error=no_code");
        }

        try {
            Map<String, Object> tokenData = exchangeCodeForTokens(code, redirectUri);
            String accessToken = (String) tokenData.get("access_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            
            if (accessToken == null) {
                logger.error("No access token in Spotify response: {}", tokenData);
                return new RedirectView(frontendUrl + "/callback?error=no_token");
            }

            Cookie cookie = new Cookie("spotify_access_token", accessToken);
            cookie.setPath("/");
            cookie.setHttpOnly(false);
            cookie.setSecure(true);
            cookie.setMaxAge(expiresIn != null ? expiresIn : 3600);
            httpResponse.addCookie(cookie);

            logger.info("Successfully exchanged code for tokens, redirecting to frontend");
            return new RedirectView(frontendUrl + "/callback?success=true");
            
        } catch (Exception e) {
            logger.error("Error processing auth callback", e);
            return new RedirectView(frontendUrl + "/callback?error=exchange_failed");
        }
    }

    @PostMapping("/auth/callback")
    public ResponseEntity<Map<String, Object>> handleAuthCallback(
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "redirect_uri", required = false) String providedRedirectUri) {
        try {
            if (code == null || code.isBlank()) {
                logger.error("No authorization code provided in POST callback");
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            
            // Use provided redirect_uri if available, otherwise use configured one
            String redirectUriToUse = providedRedirectUri != null ? providedRedirectUri : redirectUri;
            Map<String, Object> tokenData = exchangeCodeForTokens(code, redirectUriToUse);
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");

            if (accessToken == null) {
                logger.error("No access token in Spotify response: {}", tokenData);
                throw new RuntimeException("No access token received from Spotify");
            }

            logger.info("Successfully exchanged code for tokens");
            Map<String, Object> userData = spotifyApiService.getCurrentUser(accessToken);
            userService.createOrUpdateUser(userData, refreshToken);

            return ResponseEntity.ok(tokenData);
        } catch (RuntimeException e) {
            // Re-throw RuntimeException to preserve the error message
            logger.error("Error processing auth callback: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing auth callback", e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred during authentication";
            }
            throw new RuntimeException(errorMessage, e);
        }
    }

    private Map<String, Object> exchangeCodeForTokens(String code, String redirectUriToUse) {
        logger.info("Processing auth callback with code (length: {}), redirect_uri: {}", code.length(), redirectUriToUse);

        HttpHeaders headers = spotifyAuthUtil.createBasicAuthHeaders(clientId, clientSecret);

        String encodedRedirectUri;
        try {
            encodedRedirectUri = java.net.URLEncoder.encode(redirectUriToUse, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            logger.error("Failed to encode redirect URI", e);
            encodedRedirectUri = redirectUriToUse;
        }
        
        String body = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + encodedRedirectUri;
        logger.info("Exchanging code with Spotify, redirect_uri: {}", redirectUriToUse);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            SpotifyAuthUtil.SPOTIFY_TOKEN_URL, 
            HttpMethod.POST,
            entity,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        if (response.getStatusCode().isError() || response.getBody() == null) {
            logger.error("Spotify token exchange failed: Status {}, Body: {}", response.getStatusCode(), response.getBody());
            Map<String, Object> errorBody = response.getBody() != null ? response.getBody() : Map.of("error", "Unknown error");
            throw new RuntimeException("Token exchange failed: " + errorBody.get("error"));
        }

        return response.getBody();
    }

    @PostMapping("/audio-features/fetch")
    public ResponseEntity<Map<String, Object>> fetchAudioFeatures(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(defaultValue = "50") 
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit can't exceed 100")
        int limit,
        @RequestParam(required = false) 
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String timeRange) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);
        String userId = spotifyApiService.getUserId(accessToken);
        
        logger.info("Fetching audio features - timeRange: {}, limit: {}", timeRange, limit);
        
        if (timeRange != null) {
            logger.info("Using top tracks method for timeRange: {}", timeRange);
            audioFeaturesService.fetchAudioFeaturesForTopTracks(accessToken, timeRange, limit);
        } else {
            logger.info("Using listening history method (timeRange: {})", timeRange);
            audioFeaturesService.fetchMissingAudioFeatures(userId, limit);
        }
        return ResponseEntity.ok(Map.of("message", "Audio features fetched successfully"));
    }

    @GetMapping("/audio-features/insights-from-top")
    public ResponseEntity<Map<String, Object>> getAudioInsightsFromTopTracks(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam(value = "time_range", defaultValue = "medium_term")
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        String timeRange) {
        String accessToken = tokenUtil.extractAccessToken(authHeader);

        long startTime = System.currentTimeMillis();
        Map<String, Object> insights = audioFeaturesService.getAudioInsightsFromTopTracks(accessToken, timeRange);
        long durationMs = System.currentTimeMillis() - startTime;

        logger.info("Computed audio insights from top tracks in {} ms (timeRange={}, tokenHash={})",
            durationMs, timeRange, accessToken.hashCode());

        return ResponseEntity.ok(insights);
    }
    

    public static class PlaylistGenerationRequest {
        @NotBlank(message = "Time range is required")
        @Pattern(regexp = TIME_RANGE_PATTERN, message = TIME_RANGE_ERROR)
        public String time_range;
        
        @NotBlank(message = "Playlist name is required")
        public String name;
        
        public String description;
        public boolean public_playlist;
        
        public String getTime_range() { 
            return time_range; 
        }
        public void setTime_range(String time_range) { 
            this.time_range = time_range; 
        }
        
        public String getName() { 
            return name; 
        }
        public void setName(String name) { 
            this.name = name; 
        }
        
        public String getDescription() { 
            return description; 
        }
        public void setDescription(String description) { 
            this.description = description; 
        }
        
        public boolean isPublic_playlist() { 
            return public_playlist; 
        }
        public void setPublic_playlist(boolean public_playlist) { 
            this.public_playlist = public_playlist; 
        }
    }
}
