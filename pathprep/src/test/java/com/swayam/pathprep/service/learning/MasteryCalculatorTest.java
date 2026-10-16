package com.swayam.pathprep.service.learning;

import com.swayam.pathprep.enums.ConfidenceLevel;
import com.swayam.pathprep.enums.SkillStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MasteryCalculatorTest {

    private MasteryCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MasteryCalculator(20, 1, 2, 80);
    }

    @Test
    void noAttempts_returnsNull() {
        assertThat(calculator.calculateScore(null)).isNull();
        assertThat(calculator.calculateScore("")).isNull();
        assertThat(calculator.calculateStatus(null, ConfidenceLevel.LOW)).isEqualTo(SkillStatus.NOT_STARTED);
    }

    @Test
    void oneCorrect_returns66Point7() {
        String outcomes = calculator.appendOutcome("", true);
        assertThat(outcomes).isEqualTo("1");
        BigDecimal score = calculator.calculateScore(outcomes);
        assertThat(score).isEqualByComparingTo("66.7");
    }

    @Test
    void oneWrong_returns33Point3() {
        String outcomes = calculator.appendOutcome("", false);
        assertThat(outcomes).isEqualTo("0");
        BigDecimal score = calculator.calculateScore(outcomes);
        assertThat(score).isEqualByComparingTo("33.3");
    }

    @Test
    void twoWrong_returns25Point0() {
        String outcomes = calculator.appendOutcome("0", false);
        assertThat(outcomes).isEqualTo("00");
        BigDecimal score = calculator.calculateScore(outcomes);
        assertThat(score).isEqualByComparingTo("25.0");
    }

    @Test
    void windowTrimming_keepsOnlyLast20() {
        String outcomes = "";
        for (int i = 0; i < 25; i++) {
            outcomes = calculator.appendOutcome(outcomes, true);
        }
        assertThat(outcomes).hasSize(20);
        assertThat(outcomes).isEqualTo("11111111111111111111");

        outcomes = calculator.appendOutcome(outcomes, false);
        assertThat(outcomes).hasSize(20);
        assertThat(outcomes.endsWith("0")).isTrue();
    }

    @Test
    void confidenceBoundaries() {
        assertThat(calculator.calculateConfidence(0)).isEqualTo(ConfidenceLevel.LOW);
        assertThat(calculator.calculateConfidence(2)).isEqualTo(ConfidenceLevel.LOW);
        assertThat(calculator.calculateConfidence(3)).isEqualTo(ConfidenceLevel.MEDIUM);
        assertThat(calculator.calculateConfidence(7)).isEqualTo(ConfidenceLevel.MEDIUM);
        assertThat(calculator.calculateConfidence(8)).isEqualTo(ConfidenceLevel.HIGH);
    }

    @Test
    void masteredStatusRequiresScoreAndConfidenceNotLow() {
        assertThat(calculator.calculateStatus(BigDecimal.valueOf(85.0), ConfidenceLevel.LOW))
                .isEqualTo(SkillStatus.IN_PROGRESS);
        assertThat(calculator.calculateStatus(BigDecimal.valueOf(85.0), ConfidenceLevel.MEDIUM))
                .isEqualTo(SkillStatus.MASTERED);
        assertThat(calculator.calculateStatus(BigDecimal.valueOf(85.0), ConfidenceLevel.HIGH))
                .isEqualTo(SkillStatus.MASTERED);
        assertThat(calculator.calculateStatus(BigDecimal.valueOf(79.9), ConfidenceLevel.HIGH))
                .isEqualTo(SkillStatus.IN_PROGRESS);
    }
}
