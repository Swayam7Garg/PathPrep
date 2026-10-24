package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByAttemptId(Long attemptId);

    @Query("SELECT e FROM Evaluation e JOIN FETCH e.attempt a WHERE a.session.id = :sessionId")
    List<Evaluation> findBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT e FROM Evaluation e JOIN FETCH e.attempt a JOIN FETCH e.criterionScores WHERE a.session.id = :sessionId")
    List<Evaluation> findWithScoresBySessionId(@Param("sessionId") Long sessionId);
}
