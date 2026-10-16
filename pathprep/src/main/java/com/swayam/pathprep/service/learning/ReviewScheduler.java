package com.swayam.pathprep.service.learning;

import com.swayam.pathprep.enums.ReviewStatus;

import java.time.LocalDate;
import java.util.List;

public class ReviewScheduler {

    private final List<Integer> reviewIntervalsDays;

    public ReviewScheduler(List<Integer> reviewIntervalsDays) {
        this.reviewIntervalsDays = (reviewIntervalsDays != null && !reviewIntervalsDays.isEmpty())
                ? reviewIntervalsDays
                : List.of(1, 3, 7, 14);
    }

    public record ScheduleResult(
            int repetitionCount,
            Integer intervalDays,
            LocalDate nextDueOn,
            boolean graduated,
            ReviewStatus status
    ) {}

    public ScheduleResult schedule(int currentRepetitionCount, boolean correct, LocalDate today) {
        if (!correct) {
            return new ScheduleResult(
                    0,
                    1,
                    today.plusDays(1),
                    false,
                    ReviewStatus.PENDING
            );
        }

        int newRepetitionCount = currentRepetitionCount + 1;
        if (newRepetitionCount >= 4) {
            return new ScheduleResult(
                    newRepetitionCount,
                    null,
                    null,
                    true,
                    ReviewStatus.COMPLETED
            );
        }

        // Ladder intervals: [1, 3, 7, 14]
        // 0 -> 1: interval is reviewIntervalsDays.get(1) = 3
        // 1 -> 2: interval is reviewIntervalsDays.get(2) = 7
        // 2 -> 3: interval is reviewIntervalsDays.get(3) = 14
        int interval = reviewIntervalsDays.get(newRepetitionCount);
        return new ScheduleResult(
                newRepetitionCount,
                interval,
                today.plusDays(interval),
                false,
                ReviewStatus.PENDING
        );
    }
}
