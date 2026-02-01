package com.spotify.dashboard.repository;

import com.spotify.dashboard.model.TrackFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackFeaturesRepository extends JpaRepository<TrackFeatures, Long> {

    Optional<TrackFeatures> findByTrackId(String trackId);

    boolean existsByTrackId(String trackId);

    @Query("SELECT t FROM TrackFeatures t WHERE t.trackId IN :trackIds")
    List<TrackFeatures> findByTrackIdIn(@Param("trackIds") List<String> trackIds);
}