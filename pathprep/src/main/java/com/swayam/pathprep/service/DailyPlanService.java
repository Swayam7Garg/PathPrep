package com.swayam.pathprep.service;

import com.swayam.pathprep.dto.plan.request.CreateDailyPlanItemRequest;
import com.swayam.pathprep.dto.plan.response.DailyPlanItemResponse;
import com.swayam.pathprep.dto.plan.response.DailyPlanResponse;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.DailyPlanItemType;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyPlanService {

    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final CurrentUser currentUser;
    private final AppDay appDay;
    private final Clock clock;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RoleTrackRepository roleTrackRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final TrackTopicRepository trackTopicRepository;

    @Transactional(readOnly = true)
    public DailyPlanResponse getTodayPlan() {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();

        List<DailyPlanItem> items = dailyPlanItemRepository.findByUserIdAndPlanDateWithDetails(userId, today);
        List<DailyPlanItemResponse> itemResponses = items.stream()
                .map(this::toResponse)
                .toList();

        int totalItems = items.size();
        int completedItems = (int) items.stream().filter(DailyPlanItem::isCompleted).count();
        int progressPercent = totalItems > 0 ? (int) Math.round((completedItems * 100.0) / totalItems) : 0;
        boolean hasUnfinishedYesterdayTasks = dailyPlanItemRepository.hasUnfinishedTasksOnDate(userId, today.minusDays(1));

        return new DailyPlanResponse(
                today,
                totalItems,
                completedItems,
                progressPercent,
                hasUnfinishedYesterdayTasks,
                itemResponses
        );
    }

    @Transactional
    public DailyPlanItemResponse addItem(CreateDailyPlanItemRequest request) {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();

        String title;
        RoleTrack track = null;
        Topic topic = null;
        Integer questionCount = null;

        if (request.itemType() == DailyPlanItemType.CUSTOM) {
            if (request.title() == null || request.title().trim().isEmpty()) {
                throw new BadRequestException("VALIDATION_FAILED", "Title is required for custom tasks");
            }
            title = request.title().trim();
        } else if (request.itemType() == DailyPlanItemType.QUIZ) {
            if (request.topicId() == null) {
                throw new BadRequestException("VALIDATION_FAILED", "Topic ID is required for quiz tasks");
            }
            topic = topicRepository.findById(request.topicId())
                    .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic not found: " + request.topicId()));

            questionCount = request.questionCount() != null ? request.questionCount() : 5;

            if (request.trackId() != null) {
                track = roleTrackRepository.findById(request.trackId())
                        .orElseThrow(() -> new NotFoundException("TRACK_NOT_FOUND", "Track not found: " + request.trackId()));
            } else {
                List<Long> trackIds = trackTopicRepository.findTrackIdsByTopicId(topic.getId());
                if (!trackIds.isEmpty()) {
                    track = roleTrackRepository.findById(trackIds.get(0)).orElse(null);
                } else {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null && user.getTargetTrackId() != null) {
                        track = roleTrackRepository.findById(user.getTargetTrackId()).orElse(null);
                    }
                }
            }

            if (request.title() != null && !request.title().trim().isEmpty()) {
                title = request.title().trim();
            } else {
                title = "Quiz: " + topic.getName() + " (" + questionCount + " Qs)";
            }
        } else {
            throw new BadRequestException("VALIDATION_FAILED", "Invalid item type: " + request.itemType());
        }

        int maxSort = dailyPlanItemRepository.findMaxSortOrderByUserIdAndPlanDate(userId, today);
        int sortOrder = maxSort + 1;

        DailyPlanItem item = DailyPlanItem.builder()
                .user(userRepository.getReferenceById(userId))
                .planDate(today)
                .title(title)
                .itemType(request.itemType())
                .track(track)
                .topic(topic)
                .questionCount(questionCount)
                .completed(false)
                .sortOrder(sortOrder)
                .createdAt(clock.instant())
                .build();

        DailyPlanItem saved = dailyPlanItemRepository.save(item);
        log.info("Created daily plan item id={} for user={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public DailyPlanItemResponse toggleItem(Long id) {
        Long userId = currentUser.getUserId();
        DailyPlanItem item = dailyPlanItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("PLAN_ITEM_NOT_FOUND", "Plan item not found: " + id));

        boolean newStatus = !item.isCompleted();
        item.setCompleted(newStatus);
        if (newStatus) {
            item.setCompletedAt(clock.instant());
        } else {
            item.setCompletedAt(null);
            item.setSession(null);
        }

        DailyPlanItem saved = dailyPlanItemRepository.save(item);
        log.info("Toggled daily plan item id={} to completed={}", id, newStatus);
        return toResponse(saved);
    }

    @Transactional
    public void deleteItem(Long id) {
        Long userId = currentUser.getUserId();
        DailyPlanItem item = dailyPlanItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("PLAN_ITEM_NOT_FOUND", "Plan item not found: " + id));

        dailyPlanItemRepository.delete(item);
        log.info("Deleted daily plan item id={} for user={}", id, userId);
    }

    @Transactional
    public DailyPlanResponse rolloverYesterdayTasks() {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();
        LocalDate yesterday = today.minusDays(1);

        List<DailyPlanItem> yesterdayItems = dailyPlanItemRepository.findUnfinishedItemsOnDate(userId, yesterday);
        if (!yesterdayItems.isEmpty()) {
            int maxSort = dailyPlanItemRepository.findMaxSortOrderByUserIdAndPlanDate(userId, today);
            List<DailyPlanItem> rolledOverItems = new ArrayList<>();

            for (DailyPlanItem yItem : yesterdayItems) {
                maxSort++;
                DailyPlanItem newItem = DailyPlanItem.builder()
                        .user(yItem.getUser())
                        .planDate(today)
                        .title(yItem.getTitle())
                        .itemType(yItem.getItemType())
                        .track(yItem.getTrack())
                        .topic(yItem.getTopic())
                        .questionCount(yItem.getQuestionCount())
                        .completed(false)
                        .sortOrder(maxSort)
                        .createdAt(clock.instant())
                        .build();
                rolledOverItems.add(newItem);
            }

            dailyPlanItemRepository.saveAll(rolledOverItems);
            log.info("Rolled over {} tasks from {} to {} for user={}", rolledOverItems.size(), yesterday, today, userId);
        }

        return getTodayPlan();
    }

    @Transactional
    public void autoCompleteQuizItems(Long userId, Long topicId, Long sessionId, Instant completedAt) {
        LocalDate today = appDay.today();
        List<DailyPlanItem> openItems = dailyPlanItemRepository.findOpenQuizItemsByTopic(userId, today, topicId);
        if (openItems.isEmpty()) {
            return;
        }

        PracticeSession sessionRef = practiceSessionRepository.getReferenceById(sessionId);
        Instant compInstant = completedAt != null ? completedAt : clock.instant();

        for (DailyPlanItem item : openItems) {
            item.setCompleted(true);
            item.setCompletedAt(compInstant);
            item.setSession(sessionRef);
            dailyPlanItemRepository.save(item);
            log.info("Auto-completed daily plan quiz item id={} for user={} with session={}", item.getId(), userId, sessionId);
        }
    }

    private DailyPlanItemResponse toResponse(DailyPlanItem item) {
        return new DailyPlanItemResponse(
                item.getId(),
                item.getPlanDate(),
                item.getTitle(),
                item.getItemType(),
                item.getTrack() != null ? item.getTrack().getId() : null,
                item.getTrack() != null ? item.getTrack().getName() : null,
                item.getTopic() != null ? item.getTopic().getId() : null,
                item.getTopic() != null ? item.getTopic().getName() : null,
                item.getQuestionCount(),
                item.getSession() != null ? item.getSession().getId() : null,
                item.isCompleted(),
                item.getCompletedAt(),
                item.getSortOrder()
        );
    }
}
