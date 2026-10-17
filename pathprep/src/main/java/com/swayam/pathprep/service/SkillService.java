package com.swayam.pathprep.service;

import com.swayam.pathprep.config.LearningProperties;
import com.swayam.pathprep.dto.learning.*;
import com.swayam.pathprep.entity.RoleTrack;
import com.swayam.pathprep.entity.SkillMastery;
import com.swayam.pathprep.entity.Topic;
import com.swayam.pathprep.entity.TopicPrereq;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.ConfidenceLevel;
import com.swayam.pathprep.enums.SkillStatus;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.repository.projection.TopicRow;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.learning.GapRanker;
import com.swayam.pathprep.service.learning.MasteryCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final RoleTrackRepository roleTrackRepository;
    private final TopicRepository topicRepository;
    private final TopicPrereqRepository topicPrereqRepository;
    private final SkillMasteryRepository skillMasteryRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final LearningProperties learningProperties;

    @Transactional(readOnly = true)
    public SkillProfileResponse getSkillProfile(Long explicitTrackId) {
        Long userId = currentUser.getUserId();
        RoleTrack track = resolveTrack(userId, explicitTrackId);

        if (track == null) {
            return new SkillProfileResponse(null, null, List.of());
        }

        List<TopicRow> topicRows = topicRepository.findTopicsForTrack(track.getId());
        if (topicRows.isEmpty()) {
            return new SkillProfileResponse(track.getId(), track.getName(), List.of());
        }

        List<Long> topicIds = topicRows.stream().map(TopicRow::getId).toList();

        // Prerequisites mapping: topicId -> list of prereqIds
        List<TopicPrereq> prereqs = topicPrereqRepository.findByTopicIdIn(topicIds);
        Map<Long, List<Long>> prereqMap = new HashMap<>();
        Set<Long> allPrereqIds = new HashSet<>();
        for (TopicPrereq p : prereqs) {
            prereqMap.computeIfAbsent(p.getId().getTopicId(), k -> new ArrayList<>()).add(p.getId().getPrereqId());
            allPrereqIds.add(p.getId().getPrereqId());
        }

        Set<Long> neededTopicInfoIds = new HashSet<>(topicIds);
        neededTopicInfoIds.addAll(allPrereqIds);
        Map<Long, Topic> topicEntityMap = topicRepository.findAllById(neededTopicInfoIds).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));

        Map<Long, SkillMastery> masteryMap = skillMasteryRepository.findByUserIdAndTopicIdIn(userId, neededTopicInfoIds)
                .stream()
                .collect(Collectors.toMap(sm -> sm.getTopic().getId(), Function.identity()));

        MasteryCalculator masteryCalculator = new MasteryCalculator(
                learningProperties.getMasteryWindow(),
                learningProperties.getMasteryPriorCorrect(),
                learningProperties.getMasteryPriorAttempts(),
                learningProperties.getMasteredThreshold()
        );

        BigDecimal prereqThreshold = BigDecimal.valueOf(learningProperties.getPrerequisiteThreshold());

        List<TopicSkillView> topicViews = new ArrayList<>();
        for (TopicRow tr : topicRows) {
            Long tid = tr.getId();
            SkillMastery sm = masteryMap.get(tid);

            BigDecimal masteryPercent = (sm != null) ? sm.getMasteryScore() : null;
            int attemptsCount = (sm != null) ? sm.getAttemptsCount() : 0;
            int correctCount = (sm != null) ? sm.getCorrectCount() : 0;
            ConfidenceLevel confidence = (sm != null) ? sm.getConfidenceLevel() : null;
            SkillStatus status = masteryCalculator.calculateStatus(masteryPercent, confidence);

            List<Long> topicPrereqIds = prereqMap.getOrDefault(tid, List.of());
            boolean prerequisitesMet = true;
            List<UnmetPrerequisiteView> unmet = new ArrayList<>();

            for (Long pid : topicPrereqIds) {
                SkillMastery pSm = masteryMap.get(pid);
                BigDecimal pMastery = (pSm != null) ? pSm.getMasteryScore() : null;
                if (pMastery == null || pMastery.compareTo(prereqThreshold) < 0) {
                    prerequisitesMet = false;
                    Topic pTopic = topicEntityMap.get(pid);
                    String pName = (pTopic != null) ? pTopic.getName() : ("Topic " + pid);
                    unmet.add(new UnmetPrerequisiteView(pid, pName, pMastery));
                }
            }

            topicViews.add(new TopicSkillView(
                    tid,
                    tr.getName(),
                    tr.getDomain(),
                    tr.getWeight(),
                    (int) tr.getQuestionCount(),
                    masteryPercent,
                    attemptsCount,
                    correctCount,
                    confidence,
                    status,
                    prerequisitesMet,
                    unmet,
                    (sm != null) ? sm.getLastPracticedAt() : null
            ));
        }

        topicViews.sort(Comparator.comparing(TopicSkillView::weight, Comparator.reverseOrder())
                .thenComparing(TopicSkillView::name));

        return new SkillProfileResponse(track.getId(), track.getName(), topicViews);
    }

    @Transactional(readOnly = true)
    public SkillGapsResponse getSkillGaps(Long explicitTrackId, Integer limit) {
        Long userId = currentUser.getUserId();
        RoleTrack track = resolveTrack(userId, explicitTrackId);

        if (track == null) {
            return new SkillGapsResponse(null, List.of());
        }

        List<TopicRow> topicRows = topicRepository.findTopicsForTrack(track.getId());
        if (topicRows.isEmpty()) {
            return new SkillGapsResponse(track.getId(), List.of());
        }

        List<Long> topicIds = topicRows.stream().map(TopicRow::getId).toList();

        // Prerequisites mapping
        List<TopicPrereq> prereqs = topicPrereqRepository.findByTopicIdIn(topicIds);
        Map<Long, List<Long>> prereqMap = new HashMap<>();
        Set<Long> allPrereqIds = new HashSet<>();
        for (TopicPrereq p : prereqs) {
            prereqMap.computeIfAbsent(p.getId().getTopicId(), k -> new ArrayList<>()).add(p.getId().getPrereqId());
            allPrereqIds.add(p.getId().getPrereqId());
        }

        Set<Long> neededTopicInfoIds = new HashSet<>(topicIds);
        neededTopicInfoIds.addAll(allPrereqIds);

        Map<Long, SkillMastery> masteryMap = skillMasteryRepository.findByUserIdAndTopicIdIn(userId, neededTopicInfoIds)
                .stream()
                .collect(Collectors.toMap(sm -> sm.getTopic().getId(), Function.identity()));

        BigDecimal prereqThreshold = BigDecimal.valueOf(learningProperties.getPrerequisiteThreshold());

        MasteryCalculator masteryCalculator = new MasteryCalculator(
                learningProperties.getMasteryWindow(),
                learningProperties.getMasteryPriorCorrect(),
                learningProperties.getMasteryPriorAttempts(),
                learningProperties.getMasteredThreshold()
        );

        List<GapRanker.Candidate> candidates = new ArrayList<>();
        for (TopicRow tr : topicRows) {
            SkillMastery sm = masteryMap.get(tr.getId());
            BigDecimal masteryPercent = (sm != null) ? sm.getMasteryScore() : null;
            ConfidenceLevel confidence = (sm != null) ? sm.getConfidenceLevel() : null;
            SkillStatus status = masteryCalculator.calculateStatus(masteryPercent, confidence);
            int attempts = (sm != null) ? sm.getAttemptsCount() : 0;
            int correct = (sm != null) ? sm.getCorrectCount() : 0;

            List<Long> topicPrereqIds = prereqMap.getOrDefault(tr.getId(), List.of());
            boolean prerequisitesMet = true;
            for (Long pid : topicPrereqIds) {
                SkillMastery pSm = masteryMap.get(pid);
                BigDecimal pMastery = (pSm != null) ? pSm.getMasteryScore() : null;
                if (pMastery == null || pMastery.compareTo(prereqThreshold) < 0) {
                    prerequisitesMet = false;
                    break;
                }
            }

            candidates.add(new GapRanker.Candidate(
                    tr.getId(),
                    tr.getName(),
                    tr.getWeight(),
                    masteryPercent,
                    (int) tr.getQuestionCount(),
                    status,
                    attempts,
                    correct,
                    prerequisitesMet
            ));
        }

        List<GapRanker.RankedGap> ranked = new GapRanker().rank(candidates, limit);
        List<SkillGapView> gapViews = ranked.stream()
                .map(r -> new SkillGapView(
                        r.topicId(),
                        r.topicName(),
                        r.kind(),
                        r.masteryPercent(),
                        r.weight(),
                        r.impactScore(),
                        r.reason(),
                        new SuggestedAction(r.suggestedAction().type(), r.suggestedAction().topicId(), r.suggestedAction().questionCount())
                ))
                .toList();

        return new SkillGapsResponse(track.getId(), gapViews);
    }

    public RoleTrack resolveTrack(Long userId, Long explicitTrackId) {
        if (explicitTrackId != null) {
            return roleTrackRepository.findById(explicitTrackId)
                    .orElseThrow(() -> new NotFoundException("TRACK_NOT_FOUND", "Track not found: " + explicitTrackId));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getTargetTrackId() != null) {
            Optional<RoleTrack> trackOpt = roleTrackRepository.findById(user.getTargetTrackId());
            if (trackOpt.isPresent()) {
                return trackOpt.get();
            }
        }

        return practiceSessionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
                .map(com.swayam.pathprep.entity.PracticeSession::getTrack)
                .orElse(null);
    }
}
