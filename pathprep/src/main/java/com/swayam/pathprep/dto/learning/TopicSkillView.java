package com.swayam.pathprep.dto.learning;

import com.swayam.pathprep.enums.ConfidenceLevel;
import com.swayam.pathprep.enums.SkillStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TopicSkillView(
        Long topicId,
        String name,
        String domain,
        BigDecimal weight,
        int questionCount,
        BigDecimal masteryPercent,
        int attemptsCount,
        int correctCount,
        ConfidenceLevel confidence,
        SkillStatus status,
        boolean prerequisitesMet,
        List<UnmetPrerequisiteView> unmetPrerequisites,
        Instant lastPracticedAt
) {}
