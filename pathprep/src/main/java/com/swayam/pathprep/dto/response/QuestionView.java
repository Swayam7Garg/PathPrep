package com.swayam.pathprep.dto.response;

import com.swayam.pathprep.enums.QuestionType;

import java.util.List;

/**
 * Data transfer object representing a quiz question, containing prompt text, code snippets, and answer options.
 */
public record QuestionView(
    long id,
    QuestionType type,
    long topicId,
    String topicName,
    int difficulty,
    String body,
    List<OptionView> options
) {}
