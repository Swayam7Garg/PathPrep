package com.swayam.pathprep.dto.learning.response;

import com.swayam.pathprep.enums.GapKind;

/**
 * System-recommended priority topic or skill to practice based on weak areas and unmet prerequisites.
 */
public record RecommendedFocus(
        Long topicId,
        String topicName,
        GapKind kind,
        String reason
) {}
