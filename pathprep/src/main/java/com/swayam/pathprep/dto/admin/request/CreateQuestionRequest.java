package com.swayam.pathprep.dto.admin.request;

import com.swayam.pathprep.dto.admin.response.AdminQuestionOptionDto;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionRequest(
        @NotNull(message = "topicId is required")
        Long topicId,

        QuestionType type,

        @Min(value = 1, message = "Difficulty must be between 1 and 3")
        @Max(value = 3, message = "Difficulty must be between 1 and 3")
        int difficulty,

        @NotBlank(message = "Question body must not be blank")
        String body,

        String keyPoints,

        String modelAnswer,

        QuestionStatus status,

        @Size(max = 100, message = "Subtopic cannot exceed 100 characters")
        String subtopic,

        @Valid
        List<AdminQuestionOptionDto> options
) {
    public QuestionType typeOrDefault() {
        return type != null ? type : QuestionType.MCQ;
    }

    public QuestionStatus statusOrDefault() {
        return status != null ? status : QuestionStatus.DRAFT;
    }

    public String trimmedSubtopic() {
        if (subtopic == null || subtopic.isBlank()) {
            return null;
        }
        return subtopic.trim();
    }
}
