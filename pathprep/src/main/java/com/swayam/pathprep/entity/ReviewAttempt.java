package com.swayam.pathprep.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Records the result of a spaced repetition review attempt for a question, tracking recall performance, interval shifts, and easiness factors.
 */
@Entity
@Table(name = "review_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewQueueItem review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id", nullable = false)
    private QuestionOption selectedOption;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    @Column(name = "time_spent_sec", nullable = false)
    private int timeSpentSec;

    @Column(name = "step_before", nullable = false)
    private int stepBefore;

    @Column(name = "step_after", nullable = false)
    private int stepAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
