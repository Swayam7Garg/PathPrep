package com.swayam.pathprep.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents a single MCQ answer submitted by a student during a quiz session (Phase 4).
 *
 * <p>Key design decisions:
 * <ul>
 *   <li><b>Denormalised user_id:</b> stored directly to enable fast per-user attempt queries
 *       without always joining through sessions.</li>
 *   <li><b>Idempotency key:</b> a client-supplied UUID (VARCHAR 64) with a UNIQUE constraint.
 *       If the same key is seen again (network retry), the handler returns the stored result
 *       immediately without re-grading.</li>
 *   <li><b>correct flag:</b> set at insert time by Java grading logic; it reflects whether
 *       {@code selectedOption.isCorrect == true} at the moment of submission.</li>
 * </ul>
 */
@Entity
@Table(name = "attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * The quiz session this attempt belongs to. Cascade delete ensures attempts are
     * removed if the parent session is deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    /**
     * Denormalised FK to the authenticated user — avoids a join through sessions
     * when querying "all attempts by user X".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The question that was being answered. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * The option the student chose. Must belong to {@link #question};
     * this is enforced at the service layer before insert.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id", nullable = false)
    private QuestionOption selectedOption;

    /**
     * Result of Java-side grading: {@code true} iff
     * {@code selectedOption.isCorrect() == true} at submission time.
     */
    @Column(name = "correct", nullable = false)
    private boolean correct;

    /**
     * Client-supplied idempotency token (UUID recommended). The UNIQUE DB constraint
     * guarantees that concurrent or retried requests with the same key are safely deduplicated.
     */
    @Column(name = "idempotency_key", nullable = false, length = 64, unique = true)
    private String idempotencyKey;

    /** Wall-clock time when the answer was persisted. */
    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt = Instant.now();
}
