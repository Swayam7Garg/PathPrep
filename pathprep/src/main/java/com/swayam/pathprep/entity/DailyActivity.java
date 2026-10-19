package com.swayam.pathprep.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "daily_activity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_activity_user_date", columnNames = {"user_id", "activity_date"})
})
/**
 * Tracks aggregated daily learning engagement for a student (questions answered, correct count, time spent) to maintain active streaks.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "questions_answered", nullable = false)
    private int questionsAnswered = 0;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers = 0;

    @Column(name = "reviews_answered", nullable = false)
    private int reviewsAnswered = 0;

    @Column(name = "sessions_completed", nullable = false)
    private int sessionsCompleted = 0;

    @Column(name = "active_seconds", nullable = false)
    private int activeSeconds = 0;
}
