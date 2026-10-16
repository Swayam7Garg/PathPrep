package com.swayam.pathprep.dto.learning;

public record SuggestedAction(
        String type,
        Long topicId,
        int questionCount
) {}
