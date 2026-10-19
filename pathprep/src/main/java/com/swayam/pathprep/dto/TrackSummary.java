package com.swayam.pathprep.dto;

/**
 * Data transfer object summarizing a career track with total topics, questions, and track descriptions.
 */
public record TrackSummary(
    long id,
    String name,
    String description,
    long topicCount,
    long questionCount
) {}
