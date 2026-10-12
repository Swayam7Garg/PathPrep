package com.swayam.pathprep.dto;

import java.math.BigDecimal;
import java.util.List;

public record TopicInTrack(
    long topicId,
    String name,
    String domain,
    BigDecimal weight,
    long questionCount,
    List<Long> prerequisiteIds
) {}
