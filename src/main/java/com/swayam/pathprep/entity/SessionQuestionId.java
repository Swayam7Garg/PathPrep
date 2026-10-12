package com.swayam.pathprep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SessionQuestionId implements Serializable {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "position", nullable = false)
    private int position;
}
