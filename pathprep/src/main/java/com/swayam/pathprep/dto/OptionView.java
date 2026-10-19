package com.swayam.pathprep.dto;

/**
 * Data transfer object representing an answer choice rendered to the student during a quiz.
 */
public record OptionView(
    long id,
    int position,
    String text
) {}
