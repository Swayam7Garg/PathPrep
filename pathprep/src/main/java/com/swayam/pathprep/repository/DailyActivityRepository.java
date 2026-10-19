package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for managing daily student learning activity and computing practice streaks.
 */
@Repository
public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {

    Optional<DailyActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);

    @Modifying
    @Query(value = """
        INSERT INTO daily_activity (user_id, activity_date, questions_answered, correct_answers, reviews_answered, sessions_completed, active_seconds)
        VALUES (:userId, :activityDate, :questionsAnswered, :correctAnswers, :reviewsAnswered, :sessionsCompleted, :activeSeconds)
        ON DUPLICATE KEY UPDATE
            questions_answered = questions_answered + VALUES(questions_answered),
            correct_answers = correct_answers + VALUES(correct_answers),
            reviews_answered = reviews_answered + VALUES(reviews_answered),
            sessions_completed = sessions_completed + VALUES(sessions_completed),
            active_seconds = active_seconds + VALUES(active_seconds)
        """, nativeQuery = true)
    void upsertDailyActivity(
            @Param("userId") Long userId,
            @Param("activityDate") LocalDate activityDate,
            @Param("questionsAnswered") int questionsAnswered,
            @Param("correctAnswers") int correctAnswers,
            @Param("reviewsAnswered") int reviewsAnswered,
            @Param("sessionsCompleted") int sessionsCompleted,
            @Param("activeSeconds") int activeSeconds
    );

    @Query("SELECT d FROM DailyActivity d WHERE d.user.id = :userId AND d.activityDate >= :startDate ORDER BY d.activityDate ASC")
    List<DailyActivity> findByUserIdAndActivityDateGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    @Query("SELECT d.activityDate FROM DailyActivity d WHERE d.user.id = :userId AND (d.questionsAnswered + d.reviewsAnswered) >= 1 AND d.activityDate >= :startDate ORDER BY d.activityDate ASC")
    List<LocalDate> findActiveDatesInPastYear(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    @Query("SELECT COALESCE(SUM(d.questionsAnswered), 0) FROM DailyActivity d WHERE d.user.id = :userId")
    long sumAllTimeQuestionsAnswered(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.correctAnswers), 0) FROM DailyActivity d WHERE d.user.id = :userId")
    long sumAllTimeCorrectAnswers(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.activeSeconds), 0) FROM DailyActivity d WHERE d.user.id = :userId")
    long sumAllTimeActiveSeconds(@Param("userId") Long userId);

    @Query("SELECT d.questionsAnswered FROM DailyActivity d WHERE d.user.id = :userId AND d.activityDate = :activityDate")
    Optional<Integer> findQuestionsAnsweredOnDate(@Param("userId") Long userId, @Param("activityDate") LocalDate activityDate);
}
