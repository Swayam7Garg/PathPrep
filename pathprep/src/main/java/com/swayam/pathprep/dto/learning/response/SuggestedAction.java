package com.swayam.pathprep.dto.learning.response;

/**
 * Actionable suggestion guiding the student to their next most effective learning task.
 */
public record SuggestedAction(
        String type,
        Long topicId,
        int questionCount
) {}
