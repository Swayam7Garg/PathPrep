package com.swayam.pathprep.entity;

import com.swayam.pathprep.enums.ConfidenceLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "skill_mastery", uniqueConstraints = {
        @UniqueConstraint(name = "uk_skill_mastery_user_topic", columnNames = {"user_id", "topic_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "mastery_score", nullable = false, precision = 5, scale = 1)
    private BigDecimal masteryScore;

    @Column(name = "attempts_count", nullable = false)
    private int attemptsCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "recent_outcomes", nullable = false, length = 20)
    private String recentOutcomes = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 10)
    private ConfidenceLevel confidenceLevel;

    @Column(name = "last_practiced_at")
    private Instant lastPracticedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
