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
public class TopicPrereqId implements Serializable {

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "prereq_id", nullable = false)
    private Long prereqId;
}
