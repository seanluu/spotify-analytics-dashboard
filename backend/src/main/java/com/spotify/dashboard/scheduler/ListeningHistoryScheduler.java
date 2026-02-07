package com.spotify.dashboard.scheduler;

import com.spotify.dashboard.repository.UserRepository;
import com.spotify.dashboard.service.ListeningHistoryService;
import com.spotify.dashboard.service.TokenRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListeningHistoryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ListeningHistoryScheduler.class);
    private static final long THIRTY_MINUTES_MS = 1800000;

    private final ListeningHistoryService listeningHistoryService;
    private final UserRepository userRepository;
    private final TokenRefreshService tokenRefreshService;

    public ListeningHistoryScheduler(ListeningHistoryService listeningHistoryService,
                                    UserRepository userRepository,
                                    TokenRefreshService tokenRefreshService) {
        this.listeningHistoryService = listeningHistoryService;
        this.userRepository = userRepository;
        this.tokenRefreshService = tokenRefreshService;
    }

    @Scheduled(fixedRate = THIRTY_MINUTES_MS)
    public void pollRecentlyPlayedForAllUsers() {
        logger.info("Starting scheduled poll for recently played tracks");
        
        userRepository.findAll().forEach(user -> {
            try {
                String refreshToken = user.getEncryptedRefreshToken();
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    Map<String, Object> tokenData = tokenRefreshService.refreshAccessTokenForUser(user.getSpotifyId());
                    if (tokenData != null && tokenData.containsKey("access_token")) {
                        String accessToken = (String) tokenData.get("access_token");
                        listeningHistoryService.pollRecentlyPlayed(user.getSpotifyId(), accessToken);
                    }
                }
            } catch (Exception e) {
                logger.error("Error polling for user {}: {}", user.getSpotifyId(), e.getMessage());
            }
        });
        
        logger.info("Completed scheduled poll for recently played tracks");
    }
}

