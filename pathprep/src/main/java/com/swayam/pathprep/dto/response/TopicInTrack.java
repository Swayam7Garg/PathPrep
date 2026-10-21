package com.swayam.pathprep.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data transfer object detailing a curriculum topic within a track, including prerequisite statuses and question counts.
 */
public record TopicInTrack(
    long topicId,
    String name,
    String domain,
    BigDecimal weight,
    long questionCount,
    List<Long> prerequisiteIds
) {}
