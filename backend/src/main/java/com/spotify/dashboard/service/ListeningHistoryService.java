package com.spotify.dashboard.service;

import com.spotify.dashboard.model.ListeningHistory;
import com.spotify.dashboard.repository.ListeningHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ListeningHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(ListeningHistoryService.class);
    private static final int RECENTLY_PLAYED_LIMIT = 50;
    
    private final ListeningHistoryRepository repository;
    private final RestTemplate restTemplate;

    @Value("${spotify.api.base-url}")
    private String spotifyApiBaseUrl;

    public ListeningHistoryService(ListeningHistoryRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public void pollRecentlyPlayed(String userId, String accessToken) {
        try {
            Optional<LocalDateTime> lastPlayedAt = repository.findLatestPlayedAtByUserId(userId);
            Long afterTimestamp = lastPlayedAt.map(dt -> dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).orElse(null);

            String url = spotifyApiBaseUrl + "/me/player/recently-played?limit=" + RECENTLY_PLAYED_LIMIT;
            if (afterTimestamp != null) {
                url += "&after=" + afterTimestamp;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity, (Class<Map<String, Object>>)(Class<?>)Map.class);
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
            if (items == null || items.isEmpty()) {
                return;
            }
            
            int newPlaysStored = 0;
            
            for (Map<String, Object> item : items) {
                String playedAtStr = (String) item.get("played_at");
                LocalDateTime playedAt = LocalDateTime.parse(playedAtStr, DateTimeFormatter.ISO_DATE_TIME);
                
                Map<String, Object> track = (Map<String, Object>) item.get("track");
                String trackId = (String) track.get("id");
                
                if (!repository.existsByUserIdAndTrackIdAndPlayedAt(userId, trackId, playedAt)) {
                    String trackName = (String) track.get("name");
                    
                    List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                    String artistName = artists.isEmpty() ? "Unknown" : (String) artists.get(0).get("name");
                    
                    ListeningHistory history = new ListeningHistory(
                        userId, trackId, trackName, artistName, playedAt
                    );
                    repository.save(history);
                    newPlaysStored++;
                }
            }
            
            if (newPlaysStored > 0) {
                logger.info("Stored {} new plays for user {}", newPlaysStored, userId);
            }
        } catch (Exception e) {
            logger.error("Error polling recently played for user {}: {}", userId, e.getMessage());
        }
    }
}

