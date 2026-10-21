package com.swayam.pathprep.dto.learning.response;

import java.util.List;

/**
 * Comprehensive skill profile response detailing topic masteries, confidence ratings, and readiness index.
 */
public record SkillProfileResponse(
        Long trackId,
        String trackName,
        List<TopicSkillView> topics
) {}
