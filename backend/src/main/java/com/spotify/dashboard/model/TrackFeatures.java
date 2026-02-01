package com.spotify.dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "track_features", indexes = {
    @Index(name = "idx_track_id", columnList = "track_id"),
    @Index(name = "idx_fetched_at", columnList = "fetched_at")
})
public class TrackFeatures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "track_id", nullable = false, unique = true)
    private String trackId;

    @Column(name = "track_name")
    private String trackName;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "acousticness")
    private Float acousticness;

    @Column(name = "danceability")
    private Float danceability;

    @Column(name = "energy")
    private Float energy;

    @Column(name = "valence")
    private Float valence;

    @Column(name = "tempo")
    private Float tempo;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    public TrackFeatures() {}

    public TrackFeatures(String trackId, String trackName, String artistName) {
        this.trackId = trackId;
        this.trackName = trackName;
        this.artistName = artistName;
        this.fetchedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public Float getAcousticness() {
        return acousticness;
    }

    public void setAcousticness(Float acousticness) {
        this.acousticness = acousticness;
    }

    public Float getDanceability() {
        return danceability;
    }

    public void setDanceability(Float danceability) {
        this.danceability = danceability;
    }

    public Float getEnergy() {
        return energy;
    }

    public void setEnergy(Float energy) {
        this.energy = energy;
    }

    public Float getValence() {
        return valence;
    }

    public void setValence(Float valence) {
        this.valence = valence;
    }

    public Float getTempo() {
        return tempo;
    }

    public void setTempo(Float tempo) {
        this.tempo = tempo;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}

