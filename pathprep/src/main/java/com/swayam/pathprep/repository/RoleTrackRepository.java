package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.RoleTrack;
import com.swayam.pathprep.repository.projection.TrackRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for fetching career tracks and catalog summaries.
 */
@Repository
public interface RoleTrackRepository extends JpaRepository<RoleTrack, Long> {

    @Query(value = """
        SELECT 
            t.id AS id,
            t.name AS name,
            t.description AS description,
            COUNT(DISTINCT tt.topic_id) AS topicCount,
            COUNT(DISTINCT CASE WHEN q.status = 'PUBLISHED' THEN q.id ELSE NULL END) AS questionCount
        FROM role_tracks t
        LEFT JOIN track_topics tt ON tt.track_id = t.id
        LEFT JOIN questions q ON q.topic_id = tt.topic_id
        GROUP BY t.id, t.name, t.description
        ORDER BY t.name ASC
        """, nativeQuery = true)
    List<TrackRow> findAllWithCounts();
}
