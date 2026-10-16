package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.CompletionReason;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SessionReportResponse(
        Long sessionId,
        Long trackId,
        String trackName,
        QuizDifficulty difficulty,
        SessionStatus status,
        CompletionReason completionReason,
        Instant startedAt,
        Instant completedAt,
        Long elapsedSec,
        int activeTimeSec,
        Summary summary,
        BigDecimal previousScorePercent,
        List<TopicBreakdown> topics,
        List<QuestionDetail> questions,
        List<Recommendation> recommendations
) {
    public record Summary(
            int totalQuestions,
            int answeredCount,
            int correctCount,
            int unansweredCount,
            BigDecimal scorePercent,
            BigDecimal accuracyOfAnsweredPercent
    ) {}

    public record TopicBreakdown(
            Long topicId,
            String topicName,
            int questionsInTopic,
            int answered,
            int correct,
            BigDecimal accuracyPercent,
            String label
    ) {}

    public record QuestionDetail(
            int position,
            Long questionId,
            Long topicId,
            String topicName,
            String body,
            boolean answered,
            Boolean correct,
            Long selectedOptionId,
            Long correctOptionId,
            String explanation,
            String selectedOptionExplanation,
            Integer timeSpentSec,
            List<OptionView> options
    ) {}

    public record Recommendation(
            String type,
            Long topicId,
            String topicName,
            String reason,
            int suggestedQuestionCount
    ) {}
}
