package com.swayam.pathprep.mapper;

import com.swayam.pathprep.dto.TopicInTrack;
import com.swayam.pathprep.dto.TrackSummary;
import com.swayam.pathprep.repository.projection.TopicRow;
import com.swayam.pathprep.repository.projection.TrackRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper component responsible for transforming repository projection rows
 * into client-facing track and topic Data Transfer Objects (DTOs).
 */
@Slf4j
@Component
public class TrackMapper {

    /**
     * Converts a database {@link TrackRow} projection containing aggregated topic
     * and published question counts into a public {@link TrackSummary} record.
     *
     * @param row database projection row representing track metrics
     * @return transformed {@link TrackSummary} DTO, or {@code null} if row is null
     */
    public TrackSummary toSummary(TrackRow row) {
        if (row == null) {
            log.info("TrackRow is null, returning null TrackSummary");
            return null;
        }

        log.info("Mapping TrackRow [id={}, name='{}'] to TrackSummary", row.getId(), row.getName());

        return new TrackSummary(
                row.getId(),
                row.getName(),
                row.getDescription(),
                row.getTopicCount(),
                row.getQuestionCount()
        );
    }

    /**
     * Converts a database {@link TopicRow} projection and its resolved prerequisite
     * topic identifiers into a public {@link TopicInTrack} record.
     *
     * @param r               database projection row representing topic metrics
     * @param prerequisiteIds list of prerequisite topic IDs for this topic
     * @return transformed {@link TopicInTrack} DTO, or {@code null} if r is null
     */
    public TopicInTrack toTopicInTrack(TopicRow r, List<Long> prerequisiteIds) {
        if (r == null) {
            log.info("TopicRow is null, returning null TopicInTrack");
            return null;
        }

        int prereqCount = prerequisiteIds != null ? prerequisiteIds.size() : 0;
        log.info("Mapping TopicRow [id={}, name='{}'] with {} prerequisite(s) to TopicInTrack",
                r.getId(), r.getName(), prereqCount);

        return new TopicInTrack(
                r.getId(),
                r.getName(),
                r.getDomain(),
                r.getWeight(),
                r.getQuestionCount(),
                prerequisiteIds != null ? prerequisiteIds : List.of()
        );
    }
}
