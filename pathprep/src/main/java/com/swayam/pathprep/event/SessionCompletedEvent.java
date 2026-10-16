package com.swayam.pathprep.event;

import java.time.Instant;
import java.util.List;

public record SessionCompletedEvent(
    Long userId,
    Long sessionId,
    Long trackId,
    Instant completedAt,
    List<TopicResult> perTopicResults
) {
    public record TopicResult(
        Long topicId,
        int answered,
        int correct
    ) {}
}
