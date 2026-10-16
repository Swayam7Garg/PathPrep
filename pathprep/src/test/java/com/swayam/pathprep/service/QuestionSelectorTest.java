package com.swayam.pathprep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionSelectorTest {

    private QuestionSelector questionSelector;
    private Random fixedRandom;

    @BeforeEach
    void setUp() {
        fixedRandom = new Random(42);
        questionSelector = new QuestionSelector(fixedRandom);
    }

    @Test
    void select_returnsEmpty_whenNoCandidates() {
        long topicId = 1L;
        List<Long> selected = questionSelector.select(
                List.of(topicId),
                Map.of(topicId, BigDecimal.ONE),
                Map.of(),
                5
        );
        assertThat(selected).isEmpty();
    }

    @Test
    void select_returnsAll_whenCountExceedsAvailable() {
        long topic1 = 1L;
        long q1 = 101L;
        long q2 = 102L;

        List<Long> selected = questionSelector.select(
                List.of(topic1),
                Map.of(topic1, BigDecimal.ONE),
                Map.of(topic1, List.of(q1, q2)),
                10
        );

        assertThat(selected).hasSize(2)
                .containsExactlyInAnyOrder(q1, q2);
    }

    @Test
    void select_returnsExactRequestedCount_withNoDuplicates() {
        long topic1 = 1L;
        long topic2 = 2L;

        List<Long> topic1Questions = List.of(101L, 102L, 103L, 104L);
        List<Long> topic2Questions = List.of(201L, 202L, 203L, 204L);

        List<Long> selected = questionSelector.select(
                List.of(topic1, topic2),
                Map.of(topic1, BigDecimal.valueOf(2.0), topic2, BigDecimal.valueOf(1.0)),
                Map.of(topic1, topic1Questions, topic2, topic2Questions),
                4
        );

        assertThat(selected).hasSize(4);
        Set<Long> unique = new HashSet<>(selected);
        assertThat(unique).hasSize(4);
    }

    @Test
    void select_favorsHigherWeightTopics() {
        long highWeightTopic = 1L;
        long lowWeightTopic = 2L;

        List<Long> highQuestions = new ArrayList<>();
        List<Long> lowQuestions = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            highQuestions.add(100L + i);
            lowQuestions.add(200L + i);
        }

        List<Long> selected = questionSelector.select(
                List.of(highWeightTopic, lowWeightTopic),
                Map.of(highWeightTopic, BigDecimal.valueOf(3.0), lowWeightTopic, BigDecimal.valueOf(1.0)),
                Map.of(highWeightTopic, highQuestions, lowWeightTopic, lowQuestions),
                8
        );

        long highCount = selected.stream().filter(highQuestions::contains).count();
        long lowCount = selected.stream().filter(lowQuestions::contains).count();

        assertThat(highCount).isGreaterThan(lowCount);
        assertThat(selected).hasSize(8);
    }
}
