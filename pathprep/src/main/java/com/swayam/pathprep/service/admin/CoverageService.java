package com.swayam.pathprep.service.admin;

import com.swayam.pathprep.dto.admin.response.CoverageResponse;
import com.swayam.pathprep.dto.admin.response.SubtopicCoverageDto;
import com.swayam.pathprep.dto.admin.response.TopicCoverageDto;
import com.swayam.pathprep.entity.RoleTrack;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.RoleTrackRepository;
import com.swayam.pathprep.repository.TopicRepository;
import com.swayam.pathprep.repository.projection.TopicRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CoverageService {

    private final RoleTrackRepository roleTrackRepository;
    private final TopicRepository topicRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<CoverageResponse> getCoverage(Long trackId, Integer targetParam, boolean includeSubtopics) {
        int target = (targetParam != null && targetParam > 0) ? targetParam : 10;

        List<RoleTrack> tracks;
        if (trackId != null) {
            RoleTrack track = roleTrackRepository.findById(trackId)
                    .orElseThrow(() -> new NotFoundException("TRACK_NOT_FOUND", "Track not found: " + trackId));
            tracks = List.of(track);
        } else {
            tracks = roleTrackRepository.findAll();
        }

        Map<Long, List<SubtopicCoverageDto>> subtopicMap = includeSubtopics
                ? loadSubtopicBreakdown()
                : Map.of();

        List<CoverageResponse> responses = new ArrayList<>();
        for (RoleTrack track : tracks) {
            List<TopicRow> topicRows = topicRepository.findTopicsForTrack(track.getId());
            List<TopicCoverageDto> topicDtos = new ArrayList<>();
            int trackTotalQuizEligible = 0;

            for (TopicRow tr : topicRows) {
                int count = (int) tr.getQuestionCount();
                trackTotalQuizEligible += count;
                boolean meets = count >= target;
                List<SubtopicCoverageDto> subtopics = subtopicMap.getOrDefault(tr.getId(), List.of());

                topicDtos.add(new TopicCoverageDto(
                        tr.getId(),
                        tr.getName(),
                        tr.getDomain(),
                        tr.getWeight(),
                        count,
                        target,
                        meets,
                        subtopics
                ));
            }

            boolean trackMeets = !topicDtos.isEmpty() && topicDtos.stream().allMatch(TopicCoverageDto::meetsTarget);

            responses.add(new CoverageResponse(
                    track.getId(),
                    track.getName(),
                    target,
                    trackTotalQuizEligible,
                    trackMeets,
                    topicDtos
            ));
        }

        return responses;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<SubtopicCoverageDto>> loadSubtopicBreakdown() {
        String sql = """
                SELECT q.topic_id AS topic_id,
                       q.subtopic AS subtopic,
                       COUNT(q.id) AS cnt
                FROM questions q
                WHERE q.status = 'PUBLISHED'
                  AND q.type = 'MCQ'
                  AND (SELECT COUNT(*) FROM question_options qo WHERE qo.question_id = q.id) >= 2
                  AND (SELECT COUNT(*) FROM question_options qo WHERE qo.question_id = q.id AND qo.is_correct = 1) = 1
                GROUP BY q.topic_id, q.subtopic
                ORDER BY q.topic_id, cnt DESC
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        Map<Long, List<SubtopicCoverageDto>> map = new HashMap<>();

        for (Object[] row : rows) {
            Long topicId = ((Number) row[0]).longValue();
            String subtopic = (String) row[1];
            int count = ((Number) row[2]).intValue();

            map.computeIfAbsent(topicId, k -> new ArrayList<>())
                    .add(new SubtopicCoverageDto(subtopic, count));
        }

        return map;
    }
}
