package com.swayam.pathprep.entity;

import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.SessionStyle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private RoleTrack track;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private SessionMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "style", length = 20)
    private SessionStyle style;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private QuizDifficulty difficulty = QuizDifficulty.MIXED;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "current_index", nullable = false)
    private int currentIndex = 0;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<SessionQuestion> sessionQuestions = new ArrayList<>();

    public PracticeSession(
            User user,
            RoleTrack track,
            SessionMode mode,
            SessionStyle style,
            QuizDifficulty difficulty,
            int totalQuestions
    ) {
        this.user = user;
        this.track = track;
        this.mode = mode;
        this.style = style;
        this.difficulty = difficulty;
        this.totalQuestions = totalQuestions;
        this.status = SessionStatus.IN_PROGRESS;
        this.currentIndex = 0;
        this.startedAt = Instant.now();
    }
}
