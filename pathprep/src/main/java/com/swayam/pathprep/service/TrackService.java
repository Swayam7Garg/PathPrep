package com.swayam.pathprep.service;

import com.swayam.pathprep.dto.response.TopicInTrack;
import com.swayam.pathprep.dto.response.TrackSummary;
import com.swayam.pathprep.entity.TopicPrereq;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.mapper.TrackMapper;
import com.swayam.pathprep.repository.RoleTrackRepository;
import com.swayam.pathprep.repository.TopicPrereqRepository;
import com.swayam.pathprep.repository.TopicRepository;
import com.swayam.pathprep.repository.projection.TopicRow;
import com.swayam.pathprep.repository.projection.TrackRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for role track catalog discovery and syllabus retrieval.
 *
 * <p>Handles:
 * <ul>
 *   <li>Listing all active role tracks with pre-aggregated topic and question counts.</li>
 *   <li>Retrieving the syllabus for a specific track, resolving topic prerequisite relationships
 *       without N+1 database queries, and ordering topics by placement priority.</li>
 * </ul>
 */
@Slf4j
@Service
public class TrackService {

    private final RoleTrackRepository roleTrackRepository;
    private final TopicRepository topicRepository;
    private final TopicPrereqRepository topicPrereqRepository;
    private final TrackMapper trackMapper;

    public TrackService(
            RoleTrackRepository roleTrackRepository,
            TopicRepository topicRepository,
            TopicPrereqRepository topicPrereqRepository,
            TrackMapper trackMapper
    ) {
        this.roleTrackRepository = roleTrackRepository;
        this.topicRepository = topicRepository;
        this.topicPrereqRepository = topicPrereqRepository;
        this.trackMapper = trackMapper;
    }

    /**
     * Retrieves all available role tracks along with topic and published question counts.
     *
     * <p>Performance: Uses a single native query with GROUP BY aggregations (TrackRow projection)
     * so that track list rendering avoids extra queries per track.
     *
     * @return list of {@link TrackSummary} records
     */
    @Transactional(readOnly = true)
    public List<TrackSummary> listTracks() {
        log.info("Querying all role tracks with aggregated counts from database");
        List<TrackRow> rows = roleTrackRepository.findAllWithCounts();
        log.info("Found {} active role tracks in catalog", rows.size());

        return rows.stream()
                .map(trackMapper::toSummary)
                .toList();
    }

    /**
     * Retrieves all topics associated with a role track, complete with weights, question counts,
     * and prerequisite topic identifiers.
     *
     * <h3>N+1 Query Prevention Strategy:</h3>
     * <p>Instead of querying prerequisites individually for each topic inside a loop (which would cause
     * an N+1 query problem, firing 1 query for topics + N queries for prerequisites), we extract all
     * {@code topicIds} from the initial topic rows and execute a single batch query:
     * {@code findByTopicIdIn(topicIds)} using SQL {@code WHERE topic_id IN (...)}.
     *
     * <p>The results are then grouped in memory into a {@code Map<Long, List<Long>>} by topic ID,
     * providing instant O(1) lookups during DTO assembly. This limits database round-trips to
     * exactly 2 queries regardless of how many topics the track contains.
     *
     * <h3>Sorting:</h3>
     * <p>Topics are sorted by {@code weight} descending (highest placement importance first)
     * and secondarily by {@code name} ascending (alphabetical tie-breaker) to give students
     * a clear, prioritized learning roadmap.
     *
     * @param trackId the primary key of the target role track
     * @return list of {@link TopicInTrack} records sorted by priority
     * @throws NotFoundException if the track does not exist
     */
    @Transactional(readOnly = true)
    public List<TopicInTrack> getTopicsForTrack(Long trackId) {
        log.info("Verifying existence of trackId={}", trackId);
        if (!roleTrackRepository.existsById(trackId)) {
            log.info("Track verification failed: trackId={} does not exist", trackId);
            throw new NotFoundException("TRACK_NOT_FOUND", "Track not found: " + trackId);
        }

        log.info("Querying topics for trackId={}", trackId);
        List<TopicRow> rows = topicRepository.findTopicsForTrack(trackId);
        if (rows.isEmpty()) {
            log.info("Track trackId={} has no associated topics", trackId);
            return List.of();
        }

        // Extract topicIds into a list to batch-fetch all prerequisites in a single SQL IN query.
        // This avoids the N+1 query antipattern of firing a query per topic in a loop.
        List<Long> topicIds = rows.stream()
                .map(TopicRow::getId)
                .toList();
        log.info("Batch-querying prerequisites for {} topicIds: {}", topicIds.size(), topicIds);

        List<TopicPrereq> prereqs = topicPrereqRepository.findByTopicIdIn(topicIds);

        // Group prerequisites by topicId in-memory for O(1) retrieval during DTO mapping
        Map<Long, List<Long>> prereqMap = prereqs.stream()
                .collect(Collectors.groupingBy(
                        tp -> tp.getId().getTopicId(),
                        Collectors.mapping(tp -> tp.getId().getPrereqId(), Collectors.toList())
                ));

        log.info("Successfully fetched {} topics and {} prerequisite mappings for trackId={}",
                rows.size(), prereqs.size(), trackId);

        // Assemble DTOs and sort by weight descending (highest priority first), then by name
        return rows.stream()
                .map(r -> trackMapper.toTopicInTrack(r, prereqMap.getOrDefault(r.getId(), List.of())))
                .sorted(Comparator.comparing(TopicInTrack::weight, Comparator.reverseOrder())
                        .thenComparing(TopicInTrack::name))
                .toList();
    }
}
