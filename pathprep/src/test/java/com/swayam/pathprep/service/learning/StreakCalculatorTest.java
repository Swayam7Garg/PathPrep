package com.swayam.pathprep.service.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private StreakCalculator calculator;
    private final LocalDate today = LocalDate.of(2026, 10, 16);

    @BeforeEach
    void setUp() {
        calculator = new StreakCalculator();
    }

    @Test
    void emptyActiveDates_zeroStreaks() {
        var result = calculator.calculate(List.of(), today);
        assertThat(result.current()).isEqualTo(0);
        assertThat(result.longest()).isEqualTo(0);
        assertThat(result.activeToday()).isFalse();
        assertThat(result.atRisk()).isFalse();
    }

    @Test
    void onlyToday_streakOne() {
        var result = calculator.calculate(List.of(today), today);
        assertThat(result.current()).isEqualTo(1);
        assertThat(result.longest()).isEqualTo(1);
        assertThat(result.activeToday()).isTrue();
        assertThat(result.atRisk()).isFalse();
    }

    @Test
    void onlyYesterday_countsAsCurrentStreakOneAndAtRisk() {
        var result = calculator.calculate(List.of(today.minusDays(1)), today);
        assertThat(result.current()).isEqualTo(1);
        assertThat(result.longest()).isEqualTo(1);
        assertThat(result.activeToday()).isFalse();
        assertThat(result.atRisk()).isTrue();
    }

    @Test
    void gapBreaksCurrentStreak() {
        var result = calculator.calculate(List.of(today.minusDays(2)), today);
        assertThat(result.current()).isEqualTo(0);
        assertThat(result.longest()).isEqualTo(1);
        assertThat(result.activeToday()).isFalse();
        assertThat(result.atRisk()).isFalse();
    }

    @Test
    void longestVsCurrent() {
        // Run of 5 days in past, gap, then 2 days ending today
        var dates = List.of(
                today.minusDays(20),
                today.minusDays(19),
                today.minusDays(18),
                today.minusDays(17),
                today.minusDays(16),
                today.minusDays(1),
                today
        );
        var result = calculator.calculate(dates, today);
        assertThat(result.current()).isEqualTo(2);
        assertThat(result.longest()).isEqualTo(5);
        assertThat(result.activeToday()).isTrue();
        assertThat(result.atRisk()).isFalse();
    }
}
