package com.swayam.pathprep.dto.learning.response;

import com.swayam.pathprep.enums.GapKind;

import java.math.BigDecimal;

/**
 * Detailed view of an individual skill deficiency, including current mastery level and syllabus weight.
 */
public record SkillGapView(
        Long topicId,
        String topicName,
        GapKind kind,
        BigDecimal masteryPercent,
        BigDecimal weight,
        BigDecimal impactScore,
        String reason,
        SuggestedAction suggestedAction
) {}
