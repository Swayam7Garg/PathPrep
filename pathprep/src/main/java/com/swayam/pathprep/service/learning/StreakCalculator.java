package com.swayam.pathprep.service.learning;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class StreakCalculator {

    public record StreakResult(
            int current,
            int longest,
            boolean activeToday,
            boolean atRisk
    ) {}

    public StreakResult calculate(Collection<LocalDate> activeDatesList, LocalDate today) {
        Set<LocalDate> activeDates = (activeDatesList == null) ? Set.of() : new HashSet<>(activeDatesList);

        boolean activeToday = activeDates.contains(today);
        boolean activeYesterday = activeDates.contains(today.minusDays(1));

        // Current streak calculation:
        // Consecutive active days ending today OR yesterday
        int currentStreak = 0;
        if (activeToday) {
            LocalDate cursor = today;
            while (activeDates.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            }
        } else if (activeYesterday) {
            LocalDate cursor = today.minusDays(1);
            while (activeDates.contains(cursor)) {
                currentStreak++;
                cursor = cursor.minusDays(1);
            }
        }

        boolean atRisk = currentStreak > 0 && !activeToday;

        // Longest streak in the last 365 days [today - 364, today]
        int longestStreak = 0;
        int runningStreak = 0;
        LocalDate start = today.minusDays(364);

        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            if (activeDates.contains(date)) {
                runningStreak++;
                if (runningStreak > longestStreak) {
                    longestStreak = runningStreak;
                }
            } else {
                runningStreak = 0;
            }
        }

        return new StreakResult(currentStreak, longestStreak, activeToday, atRisk);
    }
}
