package com.swayam.pathprep.entity;

import com.swayam.pathprep.enums.QuestionSource;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a technical assessment question belonging to a topic.
 *
 * <p>Admin-only fields (source, subtopic, contentHash, importBatch, flagReason, reviewedAt) are
 * never included in student-facing DTOs. The quiz-eligibility rule (status=PUBLISHED, type=MCQ,
 * ≥2 options, exactly 1 correct) is enforced centrally in {@link com.swayam.pathprep.util.QuizEligibility}.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "rubric_id")
    private Long rubricId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "difficulty", nullable = false)
    private byte difficulty;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "key_points", columnDefinition = "JSON")
    private String keyPoints;

    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.DRAFT;

    // ── Admin tracking columns (V9) ───────────────────────────────────────────

    /** Provenance: how the question was created. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private QuestionSource source = QuestionSource.MANUAL;

    /**
     * Optional finer grouping within a topic (e.g. "HashMap" inside "Collections & Generics").
     * Admin-only; not included in student-facing responses or content_hash.
     */
    @Column(name = "subtopic", length = 100)
    private String subtopic;

    /**
     * SHA-256 of normalised body + options text. Used by the import pipeline to detect
     * duplicates. NULL on pre-V9 rows; computed on first edit/import thereafter.
     */
    @Column(name = "content_hash", length = 64, unique = true)
    private String contentHash;

    /** Free-text batch label set at import time (e.g. "java-batch-oct-2026"). */
    @Column(name = "import_batch", length = 64)
    private String importBatch;

    /** Reason provided by admin when flagging a question. */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    /** Timestamp when an admin last reviewed and published this question. */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuestionOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Question(Long id, Topic topic, QuestionType type, int difficulty, String body, QuestionStatus status) {
        this.id = id;
        this.topic = topic;
        this.type = type;
        this.difficulty = (byte) difficulty;
        this.body = body;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = (byte) difficulty;
    }
}
