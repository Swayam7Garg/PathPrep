package com.swayam.pathprep.repository.projection;

/**
 * Spring Data projection providing aggregated metadata and module counts for career tracks.
 */
public interface TrackRow {
    long getId();
    String getName();
    String getDescription();
    long getTopicCount();
    long getQuestionCount();
}
