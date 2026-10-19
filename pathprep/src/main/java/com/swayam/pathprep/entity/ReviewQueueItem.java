package com.swayam.pathprep.entity;

import com.swayam.pathprep.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "review_queue", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_queue_user_question", columnNames = {"user_id", "question_id"})
})
/**
 * Represents a scheduled spaced-repetition flashcard in a student's review queue, tracking SM-2 intervals, next due date, and mastery status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_attempt_id", nullable = false)
    private Attempt lastAttempt;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "repetition_count", nullable = false)
    private int repetitionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;
}
