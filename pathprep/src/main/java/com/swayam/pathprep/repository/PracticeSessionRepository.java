package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.PracticeSession;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PracticeSession s WHERE s.id = :id AND s.user.id = :userId")
    Optional<PracticeSession> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT s FROM PracticeSession s JOIN FETCH s.track WHERE s.id = :id AND s.user.id = :userId")
    Optional<PracticeSession> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Optional<PracticeSession> findFirstByUserIdAndStatus(Long userId, SessionStatus status);

    Optional<PracticeSession> findFirstByUserIdOrderByStartedAtDesc(Long userId);

    int countByUserIdAndStatus(Long userId, SessionStatus status);

    @Query("SELECT s FROM PracticeSession s JOIN FETCH s.track WHERE s.user.id = :userId AND s.status = :status ORDER BY s.completedAt DESC, s.id DESC")
    List<PracticeSession> findRecentSessionsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") SessionStatus status,
            Pageable pageable
    );

    @Query("SELECT s.scorePercent FROM PracticeSession s WHERE s.user.id = :userId AND s.track.id = :trackId AND s.status = 'COMPLETED' AND s.id <> :sessionId AND (s.completedAt < :completedAt OR (s.completedAt = :completedAt AND s.id < :sessionId)) ORDER BY s.completedAt DESC, s.id DESC LIMIT 1")
    Optional<BigDecimal> findPreviousScorePercent(
            @Param("userId") Long userId,
            @Param("trackId") Long trackId,
            @Param("sessionId") Long sessionId,
            @Param("completedAt") Instant completedAt
    );

    @Query(value = "SELECT s FROM PracticeSession s JOIN FETCH s.track t WHERE s.user.id = :userId " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:mode IS NULL OR s.mode = :mode) " +
            "AND (:trackId IS NULL OR s.track.id = :trackId)",
            countQuery = "SELECT COUNT(s) FROM PracticeSession s WHERE s.user.id = :userId " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:mode IS NULL OR s.mode = :mode) " +
            "AND (:trackId IS NULL OR s.track.id = :trackId)")
    Page<PracticeSession> findHistory(
            @Param("userId") Long userId,
            @Param("status") SessionStatus status,
            @Param("mode") SessionMode mode,
            @Param("trackId") Long trackId,
            Pageable pageable
    );
}
