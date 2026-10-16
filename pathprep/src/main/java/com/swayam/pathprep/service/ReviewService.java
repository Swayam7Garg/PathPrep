package com.swayam.pathprep.service;

import com.swayam.pathprep.config.LearningProperties;
import com.swayam.pathprep.dto.QuestionView;
import com.swayam.pathprep.dto.learning.*;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.ReviewStatus;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.exception.QuestionUnavailableException;
import com.swayam.pathprep.exception.ReviewNotDueException;
import com.swayam.pathprep.exception.ReviewNotFoundException;
import com.swayam.pathprep.mapper.QuestionMapper;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.learning.ReviewScheduler;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CurrentUser currentUser;
    private final AppDay appDay;
    private final Clock clock;
    private final LearningProperties learningProperties;
    private final ReviewQueueRepository reviewQueueRepository;
    private final ReviewAttemptRepository reviewAttemptRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final QuestionMapper questionMapper;

    @Transactional(readOnly = true)
    public DueReviewsResponse getDueReviews(Integer limitParam) {
        int limit = (limitParam == null) ? 10 : limitParam;
        if (limit < 1 || limit > 20) {
            throw new BadRequestException("INVALID_PARAMETER", "Limit must be between 1 and 20");
        }

        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();

        long dueCount = reviewQueueRepository.countDueByUserId(userId, today);
        List<ReviewQueueItem> items = reviewQueueRepository.findDueItems(userId, today, PageRequest.of(0, limit));

        List<DueReviewItem> dueItems = items.stream()
                .map(item -> {
                    Instant lastMissed = (item.getLastAttempt() != null && item.getLastAttempt().getAnsweredAt() != null)
                            ? item.getLastAttempt().getAnsweredAt()
                            : item.getCreatedAt();
                    QuestionView sanitisedView = questionMapper.toView(item.getQuestion());
                    return new DueReviewItem(
                            item.getId(),
                            item.getRepetitionCount(),
                            item.getIntervalDays(),
                            lastMissed,
                            sanitisedView
                    );
                })
                .toList();

        return new DueReviewsResponse(dueCount, dueItems);
    }

    @Transactional
    public ReviewAnswerResponse answerReview(Long reviewId, ReviewAnswerRequest request) {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();
        Instant now = clock.instant();

        // 1. Lock review item filtered by user
        ReviewQueueItem item = reviewQueueRepository.findByIdAndUserIdWithLock(reviewId, userId)
                .orElseThrow(() -> new ReviewNotFoundException("Review item not found: " + reviewId));

        // 2. Status guard: must be PENDING and dueOn <= today
        if (item.getStatus() != ReviewStatus.PENDING || item.getDueOn().isAfter(today)) {
            throw new ReviewNotDueException("Review item is not due: " + reviewId);
        }

        // 3. Question must still be PUBLISHED
        Question question = item.getQuestion();
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new QuestionUnavailableException("Question is unavailable");
        }

        // 4. Option ownership: must belong to the question
        QuestionOption selectedOption = questionOptionRepository.findById(request.selectedOptionId())
                .orElseThrow(() -> new BadRequestException("INVALID_OPTION", "Option not found: " + request.selectedOptionId()));

        if (!selectedOption.getQuestion().getId().equals(question.getId())) {
            throw new BadRequestException("INVALID_OPTION", "Option does not belong to this question");
        }

        // 5. Grade in Java
        boolean correct = selectedOption.isCorrect();

        // Correct option lookup
        QuestionOption correctOption = question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question has no correct option configured"));

        // 6. Apply schedule ladder
        int stepBefore = item.getRepetitionCount();
        ReviewScheduler scheduler = new ReviewScheduler(learningProperties.getReviewIntervalsDays());
        ReviewScheduler.ScheduleResult scheduleResult = scheduler.schedule(stepBefore, correct, today);

        item.setRepetitionCount(scheduleResult.repetitionCount());
        item.setIntervalDays(scheduleResult.intervalDays() != null ? scheduleResult.intervalDays() : item.getIntervalDays());
        if (scheduleResult.nextDueOn() != null) {
            item.setDueOn(scheduleResult.nextDueOn());
        }
        item.setStatus(scheduleResult.status());
        item.setLastReviewedAt(now);
        reviewQueueRepository.save(item);

        // 7. Persist review_attempt
        ReviewAttempt attempt = new ReviewAttempt();
        attempt.setReview(item);
        attempt.setUser(item.getUser());
        attempt.setQuestion(question);
        attempt.setSelectedOption(selectedOption);
        attempt.setCorrect(correct);
        attempt.setTimeSpentSec(request.timeSpentSec());
        attempt.setStepBefore(stepBefore);
        attempt.setStepAfter(scheduleResult.repetitionCount());
        attempt.setCreatedAt(now);
        reviewAttemptRepository.save(attempt);

        // 8. Update daily_activity: reviews_answered++ and active_seconds += min(timeSpentSec, maxSecondsPerAnswer)
        int cappedSeconds = Math.min(request.timeSpentSec(), learningProperties.getMaxSecondsPerAnswer());
        dailyActivityRepository.upsertDailyActivity(
                userId,
                today,
                0,
                0,
                1,
                0,
                cappedSeconds
        );

        // 9. Due remaining
        long dueRemaining = reviewQueueRepository.countDueByUserId(userId, today);

        return new ReviewAnswerResponse(
                reviewId,
                correct,
                selectedOption.getId(),
                correctOption.getId(),
                correctOption.getExplanation(),
                selectedOption.getExplanation(),
                new ReviewScheduleView(
                        scheduleResult.repetitionCount(),
                        scheduleResult.intervalDays(),
                        scheduleResult.nextDueOn(),
                        scheduleResult.graduated()
                ),
                dueRemaining
        );
    }
}
