package com.spotify.dashboard.scheduler;

import com.spotify.dashboard.repository.UserRepository;
import com.spotify.dashboard.service.AudioFeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AudioFeaturesScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AudioFeaturesScheduler.class);
    private static final long ONE_HOUR_MS = 3600000;
    private static final int DEFAULT_FETCH_LIMIT = 50;

    private final UserRepository userRepository;
    private final AudioFeaturesService audioFeaturesService;

    public AudioFeaturesScheduler(UserRepository userRepository,
                                 AudioFeaturesService audioFeaturesService) {
        this.userRepository = userRepository;
        this.audioFeaturesService = audioFeaturesService;
    }

    @Scheduled(fixedRate = ONE_HOUR_MS)
    public void fetchAudioFeaturesForAllUsers() {
        logger.info("Starting scheduled fetch of audio features");
        
        userRepository.findAll().forEach(user -> {
            try {
                audioFeaturesService.fetchMissingAudioFeatures(user.getSpotifyId(), DEFAULT_FETCH_LIMIT);
            } catch (Exception e) {
                logger.error("Error fetching audio features for user {}: {}", user.getSpotifyId(), e.getMessage());
            }
        });
        
        logger.info("Completed scheduled fetch of audio features");
    }
}