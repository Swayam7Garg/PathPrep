package com.swayam.pathprep.dto.learning;

import com.swayam.pathprep.enums.GapKind;

public record RecommendedFocus(
        Long topicId,
        String topicName,
        GapKind kind,
        String reason
) {}
