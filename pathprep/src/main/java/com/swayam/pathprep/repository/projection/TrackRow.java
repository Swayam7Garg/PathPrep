package com.swayam.pathprep.repository.projection;

public interface TrackRow {
    long getId();
    String getName();
    String getDescription();
    long getTopicCount();
    long getQuestionCount();
}
