package com.spotify.dashboard.controller;

import com.spotify.dashboard.service.AudioFeaturesService;
import com.spotify.dashboard.service.ListeningHistoryService;
import com.spotify.dashboard.service.SpotifyApiService;
import com.spotify.dashboard.service.UserService;
import com.spotify.dashboard.util.TokenUtil;
import com.spotify.dashboard.util.SpotifyAuthUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {SpotifyController.class}, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SpotifyController Tests")
class SpotifyControllerIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpotifyApiService spotifyApiService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ListeningHistoryService listeningHistoryService;

    @MockitoBean
    private AudioFeaturesService audioFeaturesService;

    @MockitoBean
    private TokenUtil tokenUtil;

    @MockitoBean
    private SpotifyAuthUtil spotifyAuthUtil;

    private static final String VALID_TOKEN = "valid-token-123";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @Test
    @DisplayName("Should get current user successfully")
    void shouldGetCurrentUserSuccessfully() throws Exception {
        // given
        Map<String, Object> mockUser = new HashMap<>();
        mockUser.put("id", "user123");
        mockUser.put("display_name", "Test User");

        when(tokenUtil.extractAccessToken(BEARER_TOKEN)).thenReturn(VALID_TOKEN);
        when(spotifyApiService.getCurrentUser(VALID_TOKEN)).thenReturn(mockUser);

        // when & then
        mockMvc.perform(get("/api/v1/spotify/me")
                .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.display_name").value("Test User"));
    }

    @Test
    @DisplayName("Should get top tracks with valid parameters")
    void shouldGetTopTracksWithValidParameters() throws Exception {
        // given
        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> track = new HashMap<>();
        track.put("id", "track1");
        track.put("name", "Test Track");
        mockResponse.put("items", java.util.List.of(track));

        when(tokenUtil.extractAccessToken(BEARER_TOKEN)).thenReturn(VALID_TOKEN);
        when(spotifyApiService.getTopTracks(VALID_TOKEN, "medium_term", 50))
            .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/spotify/top/tracks")
                .header("Authorization", BEARER_TOKEN)
                .param("time_range", "medium_term"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].id").value("track1"));
    }
}
