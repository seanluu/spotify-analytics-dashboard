package com.spotify.dashboard.service;

import com.spotify.dashboard.model.ListeningHistory;
import com.spotify.dashboard.model.TrackFeatures;
import com.spotify.dashboard.repository.ListeningHistoryRepository;
import com.spotify.dashboard.repository.TrackFeaturesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AudioFeaturesService {

    private static final Logger logger = LoggerFactory.getLogger(AudioFeaturesService.class);

    private static final int RATE_LIMIT_DELAY_MS = 50;
    private static final int BATCH_DELAY_MS = 200;
    private static final int DEFAULT_TOP_TRACKS_LIMIT = 50;
    private static final int BATCH_SIZE = 40;
    private static final String RECCOBEATS_API_BASE = "https://api.reccobeats.com/v1";
    
    // Mood thresholds
    private static final double HIGH_VALENCE_THRESHOLD = 0.6;
    private static final double LOW_VALENCE_THRESHOLD = 0.4;
    private static final double HIGH_ENERGY_THRESHOLD = 0.6;

    private final TrackFeaturesRepository trackFeaturesRepository;
    private final ListeningHistoryRepository listeningHistoryRepository;
    private final RestTemplate restTemplate;
    private final SpotifyApiService spotifyApiService;

    public AudioFeaturesService(TrackFeaturesRepository trackFeaturesRepository,
                               ListeningHistoryRepository listeningHistoryRepository,
                               RestTemplate restTemplate,
                               SpotifyApiService spotifyApiService) {
        this.trackFeaturesRepository = trackFeaturesRepository;
        this.listeningHistoryRepository = listeningHistoryRepository;
        this.restTemplate = restTemplate;
        this.spotifyApiService = spotifyApiService;
    }

    public void fetchMissingAudioFeatures(String userId, int limit) {
        try {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            LocalDateTime now = LocalDateTime.now();
            List<ListeningHistory> recentTracks = listeningHistoryRepository
                .findByUserIdAndDateRange(userId, oneMonthAgo, now);

            if (recentTracks.isEmpty()) {
                logger.info("No recent tracks found for user {}", userId);
                return;
            }

            List<String> trackIds = recentTracks.stream()
                .map(ListeningHistory::getTrackId)
                .distinct()
                .filter(trackId -> !trackFeaturesRepository.existsByTrackId(trackId))
                .limit(limit)
                .collect(Collectors.toList());

            if (trackIds.isEmpty()) {
                logger.info("All recent tracks already have audio features for user {}", userId);
                return;
            }

            int saved = fetchAndSaveAudioFeatures(trackIds, recentTracks);
            logger.info("Saved audio features for {} out of {} tracks using ReccoBeats API", saved, trackIds.size());

        } catch (Exception e) {
            logger.error("Error fetching audio features for user {}: {}", userId, e.getMessage());
        }
    }

    private Map<String, Object> buildInsightsResponse(List<String> trackIds, List<TrackFeatures> trackFeatures,
                                                      Map<String, Double> averages, Map<String, Object> periodInfo) {
        Map<String, Object> insights = new HashMap<>();
        insights.put("period", periodInfo);
        insights.put("totalTracks", trackIds.size());
        insights.put("tracksWithFeatures", trackFeatures.size());
        insights.put("averages", Map.of(
            "energy", round(averages.get("energy"), 2),
            "valence", round(averages.get("valence"), 2),
            "danceability", round(averages.get("danceability"), 2),
            "tempo", round(averages.get("tempo"), 1),
            "acousticness", round(averages.get("acousticness"), 2)
        ));
        insights.put("mood", determineMood(averages.get("valence"), averages.get("energy")));
        
        return insights;
    }


    private int fetchAndSaveAudioFeatures(List<String> trackIds, List<ListeningHistory> recentTracks) {
        int saved = 0;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        for (int i = 0; i < trackIds.size(); i += BATCH_SIZE) {
            try {
                List<String> batch = trackIds.subList(i, Math.min(i + BATCH_SIZE, trackIds.size()));
                List<Map<String, Object>> reccoTracks = fetchReccoBeatsIds(batch, entity);
                
                if (reccoTracks != null) {
                    for (Map<String, Object> track : reccoTracks) {
                        String reccobeatsId = (String) track.get("id");
                        if (reccobeatsId == null) continue;
                        
                        Map<String, Object> features = fetchAudioFeatures(reccobeatsId, entity);
                        if (features == null) continue;
                        
                        String spotifyId = extractSpotifyId((String) track.get("href"));
                        if (spotifyId == null) continue;
                        
                        TrackFeatures trackFeature = mapToTrackFeatures(features, recentTracks, spotifyId);
                        if (trackFeature != null) {
                            trackFeaturesRepository.save(trackFeature);
                            saved++;
                        }
                        Thread.sleep(RATE_LIMIT_DELAY_MS); // rate limiting for ReccoBeats API
                    }
                }
                Thread.sleep(BATCH_DELAY_MS); // delay between batches to avoid overwhelming the API
            } catch (Exception e) {
                logger.warn("Failed to process batch starting at index {}: {}", i, e.getMessage());
            }
        }
        return saved;
    }


    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchReccoBeatsIds(List<String> spotifyIds, HttpEntity<String> entity) {
        try {
            String queryParams = spotifyIds.stream()
                .map(id -> "ids=" + id)
                .collect(Collectors.joining("&"));
            String url = RECCOBEATS_API_BASE + "/track?" + queryParams;
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, (Class<Map<String, Object>>)(Class<?>)Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("content");
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch ReccoBeats IDs: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAudioFeatures(String reccobeatsId, HttpEntity<String> entity) {
        try {
            String url = RECCOBEATS_API_BASE + "/track/" + reccobeatsId + "/audio-features";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, (Class<Map<String, Object>>)(Class<?>)Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch audio features for ReccoBeats ID {}: {}", reccobeatsId, e.getMessage());
        }
        return null;
    }

    private String extractSpotifyId(String spotifyHref) {
        if (spotifyHref != null && spotifyHref.contains("/track/")) {
            return spotifyHref.substring(spotifyHref.lastIndexOf("/") + 1);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void fetchAudioFeaturesForTopTracks(String accessToken, String timeRange, int limit) {
        Map<String, Object> response = spotifyApiService.getTopTracks(accessToken, timeRange, limit);
        if (response == null || response.get("items") == null) {
            return;
        }
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.get("items");
        if (tracks == null || tracks.isEmpty()) {
            return;
        }

        List<String> trackIds = tracks.stream()
            .map(track -> (String) track.get("id"))
            .filter(trackId -> !trackFeaturesRepository.existsByTrackId(trackId))
            .collect(Collectors.toList());

        if (trackIds.isEmpty()) {
            logger.info("All top tracks already have audio features for time range: {}", timeRange);
            return;
        }

        int saved = fetchAndSaveAudioFeatures(trackIds, Collections.emptyList());
        logger.info("Saved audio features for {} out of {} top tracks for time range {}", saved, trackIds.size(), timeRange);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAudioInsightsFromTopTracks(String accessToken, String timeRange) {
        Map<String, Object> response = spotifyApiService.getTopTracks(accessToken, timeRange, DEFAULT_TOP_TRACKS_LIMIT);
        if (response == null || response.get("items") == null) {
            return Map.of("message", "No top tracks found for this period");
        }
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.get("items");
        if (tracks == null || tracks.isEmpty()) {
            return Map.of("message", "No top tracks found for this period");
        }

        List<String> trackIds = tracks.stream()
            .map(track -> (String) track.get("id"))
            .collect(Collectors.toList());

        List<TrackFeatures> trackFeatures = trackFeaturesRepository.findByTrackIdIn(trackIds);

        if (trackFeatures.isEmpty()) {
            return Map.of(
                "message", "No audio features available yet. Click 'Update Features' to fetch them.",
                "totalTracks", trackIds.size(),
                "tracksWithFeatures", 0
            );
        }

        Map<String, Double> averages = calculateAverages(trackFeatures);
        
        return buildInsightsResponse(trackIds, trackFeatures, averages, 
            Map.of("timeRange", timeRange));
    }
    
    private TrackFeatures mapToTrackFeatures(Map<String, Object> features, List<ListeningHistory> listeningHistory, String spotifyId) {
        try {
            Optional<ListeningHistory> historyEntry = listeningHistory.stream()
                .filter(h -> h.getTrackId().equals(spotifyId))
                .findFirst();

            TrackFeatures trackFeature = new TrackFeatures(
                spotifyId,
                historyEntry.map(ListeningHistory::getTrackName).orElse("Unknown"),
                historyEntry.map(ListeningHistory::getArtistName).orElse("Unknown")
            );

            // Map float fields (only the ones we actually use)
            Map<String, java.util.function.BiConsumer<TrackFeatures, Float>> floatFields = Map.of(
                "acousticness", TrackFeatures::setAcousticness,
                "danceability", TrackFeatures::setDanceability,
                "energy", TrackFeatures::setEnergy,
                "valence", TrackFeatures::setValence,
                "tempo", TrackFeatures::setTempo
            );
            floatFields.forEach((key, setter) -> setFloatField(trackFeature, features, key, setter));

            return trackFeature;
        } catch (Exception e) {
            logger.error("Error mapping track features: {}", e.getMessage());
            return null;
        }
    }

    private void setFloatField(TrackFeatures trackFeature, Map<String, Object> features, 
                               String key, java.util.function.BiConsumer<TrackFeatures, Float> setter) {
        Float value = getFloatValue(features.get(key));
        if (value != null) {
            setter.accept(trackFeature, value);
        }
    }

    private Float getFloatValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).floatValue();
        return null;
    }
    
    private Map<String, Double> calculateAverages(List<TrackFeatures> trackFeatures) {
        Map<String, Double> averages = new HashMap<>();
        
        Map<String, java.util.function.Function<TrackFeatures, Float>> featureExtractors = Map.of(
            "energy", TrackFeatures::getEnergy,
            "valence", TrackFeatures::getValence,
            "danceability", TrackFeatures::getDanceability,
            "tempo", TrackFeatures::getTempo,
            "acousticness", TrackFeatures::getAcousticness
        );
        
        featureExtractors.forEach((key, extractor) -> 
            averages.put(key, calculateAverage(trackFeatures, extractor))
        );
        
        return averages;
    }

    private double calculateAverage(List<TrackFeatures> trackFeatures,
                                    java.util.function.Function<TrackFeatures, Float> extractor) {
        // Simple average: each unique song counts equally
        return trackFeatures.stream()
            .mapToDouble(t -> {
                Float value = extractor.apply(t);
                return value != null ? value : 0.0;
            })
            .average()
            .orElse(0.0);
    }

    private String determineMood(double valence, double energy) {
        boolean isHighValence = valence > HIGH_VALENCE_THRESHOLD;
        boolean isLowValence = valence <= LOW_VALENCE_THRESHOLD;
        boolean isHighEnergy = energy > HIGH_ENERGY_THRESHOLD;
        
        if (isHighValence && isHighEnergy) return "Happy & Energetic";
        if (isHighValence) return "Happy & Calm";
        if (isLowValence && isHighEnergy) return "Angry & Energetic";
        if (isLowValence) return "Sad & Calm";
        return "Neutral";
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

}

