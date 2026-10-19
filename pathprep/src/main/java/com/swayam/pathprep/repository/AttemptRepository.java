package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for persisting and querying student quiz answer attempts.
 */
@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    Optional<Attempt> findByIdempotencyKey(String idempotencyKey);

    List<Attempt> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    List<Attempt> findBySessionId(Long sessionId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndCorrectTrue(Long sessionId);

    @Query("SELECT COALESCE(SUM(a.timeSpentSec), 0) FROM Attempt a WHERE a.session.id = :sessionId")
    int sumTimeSpentSecBySessionId(@Param("sessionId") Long sessionId);
}
