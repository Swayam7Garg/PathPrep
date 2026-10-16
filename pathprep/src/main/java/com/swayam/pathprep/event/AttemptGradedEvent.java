package com.swayam.pathprep.event;

import com.swayam.pathprep.entity.Attempt;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AttemptGradedEvent extends ApplicationEvent {

    private final Attempt attempt;
    private final Long userId;
    private final Long sessionId;
    private final Long attemptId;
    private final Long questionId;
    private final Long topicId;
    private final boolean correct;
    private final int timeSpentSec;

    public AttemptGradedEvent(
            Object source,
            Attempt attempt,
            Long userId,
            Long sessionId,
            Long attemptId,
            Long questionId,
            Long topicId,
            boolean correct,
            int timeSpentSec
    ) {
        super(source);
        this.attempt = attempt;
        this.userId = userId;
        this.sessionId = sessionId;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.topicId = topicId;
        this.correct = correct;
        this.timeSpentSec = timeSpentSec;
    }

    public AttemptGradedEvent(Object source, Attempt attempt) {
        this(
                source,
                attempt,
                attempt.getUser() != null ? attempt.getUser().getId() : null,
                attempt.getSession() != null ? attempt.getSession().getId() : null,
                attempt.getId(),
                attempt.getQuestion() != null ? attempt.getQuestion().getId() : null,
                (attempt.getQuestion() != null && attempt.getQuestion().getTopic() != null)
                        ? attempt.getQuestion().getTopic().getId()
                        : null,
                attempt.isCorrect(),
                attempt.getTimeSpentSec() != null ? attempt.getTimeSpentSec() : 0
        );
    }
}
