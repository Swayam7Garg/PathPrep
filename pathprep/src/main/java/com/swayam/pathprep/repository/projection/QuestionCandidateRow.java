package com.swayam.pathprep.repository.projection;

/**
 * Spring Data projection representing candidate questions eligible for practice session selection.
 */
public interface QuestionCandidateRow {
    long getId();
    long getTopicId();
}
