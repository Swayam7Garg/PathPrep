package com.swayam.pathprep.dto.admin.response;

import java.util.List;

public record CoverageResponse(
        Long trackId,
        String trackName,
        int targetPerTopic,
        int totalQuizEligible,
        boolean meetsTarget,
        List<TopicCoverageDto> topics
) {}
