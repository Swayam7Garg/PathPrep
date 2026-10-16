package com.swayam.pathprep.dto.learning;

import com.swayam.pathprep.enums.GapKind;

import java.math.BigDecimal;

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
