package com.spotify.dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listening_history", indexes = {
    @Index(name = "idx_user_played_at", columnList = "user_id,played_at")
})
public class ListeningHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "track_id", nullable = false)
    private String trackId;

    @Column(name = "track_name", nullable = false)
    private String trackName;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    public ListeningHistory() {}

    public ListeningHistory(String userId, String trackId, String trackName, String artistName, LocalDateTime playedAt) {
        this.userId = userId;
        this.trackId = trackId;
        this.trackName = trackName;
        this.artistName = artistName;
        this.playedAt = playedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }

    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { 
        this.playedAt = playedAt;
    }
}