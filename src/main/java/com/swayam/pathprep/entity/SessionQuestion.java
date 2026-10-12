package com.swayam.pathprep.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionQuestion {

    @EmbeddedId
    private SessionQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    public SessionQuestion(PracticeSession session, Question question, int position) {
        this.session = session;
        this.question = question;
        this.id = new SessionQuestionId(session.getId(), position);
    }

    public int getPosition() {
        return id != null ? id.getPosition() : 0;
    }
}
