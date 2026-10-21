package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.ReviewQueueItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for querying and updating SM-2 spaced repetition queue items due for review.
 */
@Repository
public interface ReviewQueueRepository extends JpaRepository<ReviewQueueItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rq FROM ReviewQueueItem rq WHERE rq.id = :id AND rq.user.id = :userId")
    Optional<ReviewQueueItem> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT rq FROM ReviewQueueItem rq WHERE rq.id = :id AND rq.user.id = :userId")
    Optional<ReviewQueueItem> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT rq FROM ReviewQueueItem rq WHERE rq.user.id = :userId AND rq.question.id = :questionId")
    Optional<ReviewQueueItem> findByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId);

    boolean existsByQuestionId(Long questionId);

    @Query("""
        SELECT COUNT(rq)
        FROM ReviewQueueItem rq
        WHERE rq.user.id = :userId
          AND rq.status = 'PENDING'
          AND rq.dueOn <= :today
          AND rq.question.status = com.swayam.pathprep.enums.QuestionStatus.PUBLISHED
          AND rq.question.type = com.swayam.pathprep.enums.QuestionType.MCQ
          AND SIZE(rq.question.options) >= 2
          AND (SELECT COUNT(qo) FROM QuestionOption qo WHERE qo.question = rq.question AND qo.isCorrect = true) = 1
        """)
    long countDueByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("""
        SELECT rq
        FROM ReviewQueueItem rq
        JOIN FETCH rq.question q
        LEFT JOIN FETCH rq.lastAttempt la
        WHERE rq.user.id = :userId
          AND rq.status = 'PENDING'
          AND rq.dueOn <= :today
          AND q.status = com.swayam.pathprep.enums.QuestionStatus.PUBLISHED
          AND q.type = com.swayam.pathprep.enums.QuestionType.MCQ
          AND SIZE(q.options) >= 2
          AND (SELECT COUNT(qo) FROM QuestionOption qo WHERE qo.question = q AND qo.isCorrect = true) = 1
        ORDER BY rq.dueOn ASC, rq.id ASC
        """)
    List<ReviewQueueItem> findDueItems(@Param("userId") Long userId, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT COUNT(rq) FROM ReviewQueueItem rq WHERE rq.user.id = :userId")
    long countAllEverQueuedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(rq) FROM ReviewQueueItem rq WHERE rq.user.id = :userId AND rq.status = 'COMPLETED'")
    long countGraduatedByUserId(@Param("userId") Long userId);
}
