package com.swayam.pathprep.service.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReadinessCalculatorTest {

    private ReadinessCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ReadinessCalculator(10, 5);
    }

    @Test
    void below10Answers_returnsNullAndReasonNotEnoughData() {
        var res = calculator.calculate(9, List.of(), 0, 0);
        assertThat(res.percent()).isNull();
        assertThat(res.reason()).isEqualTo("NOT_ENOUGH_DATA");
        assertThat(res.basedOnQuestions()).isEqualTo(9);
    }

    @Test
    void coverageCountsUntouchedTopicsAsZero() {
        var t1 = new ReadinessCalculator.TopicWeightAndMastery(BigDecimal.valueOf(2.0), BigDecimal.valueOf(80.0), 5);
        var t2 = new ReadinessCalculator.TopicWeightAndMastery(BigDecimal.valueOf(1.0), null, 3); // untouched

        // Total weight = 3.0. weightedMastery = 2.0*80 + 1.0*0 = 160. trackMastery = 160 / 3 = 53.333 -> 53.3
        var res = calculator.calculate(15, List.of(t1, t2), 0, 2); // < 5 mistakes -> only trackMastery
        assertThat(res.percent()).isEqualByComparingTo("53.3");
        assertThat(res.reason()).isNull();
    }

    @Test
    void clearanceUsedOnlyWithAtLeastFiveQueuedMistakes() {
        var t1 = new ReadinessCalculator.TopicWeightAndMastery(BigDecimal.valueOf(1.0), BigDecimal.valueOf(80.0), 5);

        // Under 5 mistakes (e.g. 4 ever queued): clearance ignored
        var res1 = calculator.calculate(15, List.of(t1), 4, 4);
        assertThat(res1.percent()).isEqualByComparingTo("80.0");

        // 5 mistakes ever queued, 4 graduated: clearance = 4/5 * 100 = 80%.
        // readiness = 0.8 * 80.0 + 0.2 * 80.0 = 80.0
        var res2 = calculator.calculate(15, List.of(t1), 4, 5);
        assertThat(res2.percent()).isEqualByComparingTo("80.0");

        // 5 mistakes ever queued, 0 graduated: clearance = 0%.
        // readiness = 0.8 * 80.0 + 0.2 * 0 = 64.0
        var res3 = calculator.calculate(15, List.of(t1), 0, 5);
        assertThat(res3.percent()).isEqualByComparingTo("64.0");
    }
    @Test
    void studentWithAnswersButNoQueuedMistakes_calculatesWithoutDivisionByZero() {
        var t1 = new ReadinessCalculator.TopicWeightAndMastery(BigDecimal.valueOf(1.0), BigDecimal.valueOf(85.0), 5);
        var res = calculator.calculate(20, List.of(t1), 0, 0); // 0 ever queued
        assertThat(res.percent()).isEqualByComparingTo("85.0");
        assertThat(res.reason()).isNull();
        assertThat(res.basedOnQuestions()).isEqualTo(20);
    }
}
