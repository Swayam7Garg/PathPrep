package com.swayam.pathprep.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines a directed prerequisite dependency between two topics, enforcing structured curriculum learning orders.
 */
@Entity
@Table(name = "topic_prereqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopicPrereq {

    @EmbeddedId
    private TopicPrereqId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("prereqId")
    @JoinColumn(name = "prereq_id", nullable = false)
    private Topic prereq;

    public TopicPrereq(Topic topic, Topic prereq) {
        this.topic = topic;
        this.prereq = prereq;
        this.id = new TopicPrereqId(topic.getId(), prereq.getId());
    }
}
