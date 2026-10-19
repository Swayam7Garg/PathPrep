package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Topic;
import com.swayam.pathprep.repository.projection.TopicRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for querying curriculum topics and module metadata.
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Query(value = """
        SELECT 
            t.id AS id,
            t.name AS name,
            t.domain AS domain,
            tt.weight AS weight,
            COUNT(DISTINCT CASE WHEN q.status = 'PUBLISHED' THEN q.id ELSE NULL END) AS questionCount
        FROM track_topics tt
        JOIN topics t ON t.id = tt.topic_id
        LEFT JOIN questions q ON q.topic_id = t.id
        WHERE tt.track_id = :trackId
        GROUP BY t.id, t.name, t.domain, tt.weight
        """, nativeQuery = true)
    List<TopicRow> findTopicsForTrack(@Param("trackId") Long trackId);
}
