package com.swayam.pathprep.dto;

public record TrackSummary(
    long id,
    String name,
    String description,
    long topicCount,
    long questionCount
) {}
