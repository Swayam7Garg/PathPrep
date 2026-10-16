package com.swayam.pathprep.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TopicPerformanceEvaluatorTest {

    @Test
    void evaluateLabel_whenFewerThanTwoAnswered_returnsNotEnoughData() {
        assertThat(TopicPerformanceEvaluator.evaluateLabel(0, BigDecimal.ZERO))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_NOT_ENOUGH_DATA);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(1, BigDecimal.valueOf(100.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_NOT_ENOUGH_DATA);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(1, BigDecimal.valueOf(0.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_NOT_ENOUGH_DATA);
    }

    @Test
    void evaluateLabel_whenAccuracyAtOrAbove80_returnsStrong() {
        assertThat(TopicPerformanceEvaluator.evaluateLabel(2, BigDecimal.valueOf(80.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_STRONG);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(5, BigDecimal.valueOf(100.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_STRONG);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(10, BigDecimal.valueOf(85.5)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_STRONG);
    }

    @Test
    void evaluateLabel_whenAccuracyBelow50_returnsWeak() {
        assertThat(TopicPerformanceEvaluator.evaluateLabel(2, BigDecimal.valueOf(0.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_WEAK);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(4, BigDecimal.valueOf(25.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_WEAK);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(3, BigDecimal.valueOf(49.9)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_WEAK);
    }

    @Test
    void evaluateLabel_whenAccuracyBetween50And79_returnsOk() {
        assertThat(TopicPerformanceEvaluator.evaluateLabel(2, BigDecimal.valueOf(50.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_OK);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(4, BigDecimal.valueOf(75.0)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_OK);
        assertThat(TopicPerformanceEvaluator.evaluateLabel(5, BigDecimal.valueOf(79.9)))
                .isEqualTo(TopicPerformanceEvaluator.LABEL_OK);
    }
}
