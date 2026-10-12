package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.QuestionType;

import java.util.List;

public record QuestionView(
    long id,
    QuestionType type,
    long topicId,
    String topicName,
    int difficulty,
    String body,
    List<OptionView> options
) {}
