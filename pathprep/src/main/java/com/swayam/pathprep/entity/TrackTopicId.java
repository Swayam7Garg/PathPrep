package com.swayam.pathprep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * Composite primary key for the TrackTopic join entity, combining track ID and topic ID.
 */
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
