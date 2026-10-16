package com.swayam.pathprep.dto.learning;

import java.util.List;

public record SkillProfileResponse(
        Long trackId,
        String trackName,
        List<TopicSkillView> topics
) {}
