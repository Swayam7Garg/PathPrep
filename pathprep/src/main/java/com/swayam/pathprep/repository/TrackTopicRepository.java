package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.TrackTopic;
import com.swayam.pathprep.entity.TrackTopicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for querying topic mappings, weights, and ordering within career tracks.
 */
@Repository
public interface TrackTopicRepository extends JpaRepository<TrackTopic, TrackTopicId> {

    @Query("SELECT tt FROM TrackTopic tt WHERE tt.id.trackId = :trackId")
    List<TrackTopic> findByTrackId(@Param("trackId") Long trackId);

    @Query("SELECT tt.id.trackId FROM TrackTopic tt WHERE tt.id.topicId = :topicId")
    List<Long> findTrackIdsByTopicId(@Param("topicId") Long topicId);
}

