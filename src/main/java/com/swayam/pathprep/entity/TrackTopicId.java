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
public class TrackTopicId implements Serializable {

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;
}
