package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.SessionStatus;

/**
 * Response payload for {@code POST /api/v1/sessions/{id}/answers}.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code correct} — whether the submitted option was the right answer</li>
 *   <li>{@code correctOptionId} — the ID of the correct option (always present so the
 *       client can highlight the right answer regardless of what was chosen)</li>
 *   <li>{@code explanation} — the explanation text from the correct option, may be null
 *       if the question author did not supply one</li>
 *   <li>{@code sessionStatus} — updated session status; {@code COMPLETED} when the last
 *       question has been answered</li>
 *   <li>{@code currentIndex} — updated pointer into the question list after this submission</li>
 *   <li>{@code totalQuestions} — total number of questions in the session (unchanged)</li>
 *   <li>{@code nextQuestion} — the next {@link QuestionView} (sanitized, no answer keys),
 *       or {@code null} when the session is {@code COMPLETED}</li>
 * </ul>
 */
public record AnswerResponse(
        boolean correct,
        long correctOptionId,
        String explanation,
        SessionStatus sessionStatus,
        int currentIndex,
        int totalQuestions,
        QuestionView nextQuestion   // null when session is COMPLETED
) {}
