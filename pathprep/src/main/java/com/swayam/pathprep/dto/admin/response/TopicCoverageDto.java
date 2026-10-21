package com.swayam.pathprep.dto.admin.response;

import java.math.BigDecimal;
import java.util.List;

public record TopicCoverageDto(
        Long topicId,
        String topicName,
        String domain,
        BigDecimal weight,
        int quizEligibleCount,
        int target,
        boolean meetsTarget,
        List<SubtopicCoverageDto> subtopics
) {}
