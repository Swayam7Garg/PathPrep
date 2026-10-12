package com.swayam.pathprep.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "track_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackTopic {

    @EmbeddedId
    private TrackTopicId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("trackId")
    @JoinColumn(name = "track_id", nullable = false)
    private RoleTrack track;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal weight = BigDecimal.valueOf(1.00);

    public TrackTopic(RoleTrack track, Topic topic, BigDecimal weight) {
        this.track = track;
        this.topic = topic;
        this.id = new TrackTopicId(track.getId(), topic.getId());
        this.weight = weight;
    }
}
