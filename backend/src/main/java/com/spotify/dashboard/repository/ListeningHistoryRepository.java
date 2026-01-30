package com.spotify.dashboard.repository;

import com.spotify.dashboard.model.ListeningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListeningHistoryRepository extends JpaRepository<ListeningHistory, Long> {

    @Query("SELECT MAX(l.playedAt) FROM ListeningHistory l WHERE l.userId = :userId")
    Optional<LocalDateTime> findLatestPlayedAtByUserId(@Param("userId") String userId);

    @Query("SELECT l FROM ListeningHistory l " +
           "WHERE l.userId = :userId AND l.playedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY l.playedAt DESC")
    List<ListeningHistory> findByUserIdAndDateRange(@Param("userId") String userId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    boolean existsByUserIdAndTrackIdAndPlayedAt(String userId, String trackId, LocalDateTime playedAt);
}