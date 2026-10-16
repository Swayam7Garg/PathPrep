package com.swayam.pathprep.service.learning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ReadinessCalculator {

    private final int minAnswersForReadiness;
    private final int minMistakesForClearance;

    public ReadinessCalculator(int minAnswersForReadiness, int minMistakesForClearance) {
        this.minAnswersForReadiness = minAnswersForReadiness;
        this.minMistakesForClearance = minMistakesForClearance;
    }

    public record TopicWeightAndMastery(
            BigDecimal weight,
            BigDecimal masteryPercent,
            int publishedQuestionCount
    ) {}

    public record ReadinessResult(
            BigDecimal percent,
            String reason,
            int basedOnQuestions
    ) {}

    public ReadinessResult calculate(
            int totalQuestionsAnswered,
            List<TopicWeightAndMastery> topics,
            long graduatedMistakesCount,
            long totalEverQueuedMistakesCount
    ) {
        if (totalQuestionsAnswered < minAnswersForReadiness) {
            return new ReadinessResult(null, "NOT_ENOUGH_DATA", totalQuestionsAnswered);
        }

        // Filter track topics that have at least one PUBLISHED question
        List<TopicWeightAndMastery> activeTopics = (topics == null) ? List.of() : topics.stream()
                .filter(t -> t.publishedQuestionCount() > 0)
                .toList();

        if (activeTopics.isEmpty()) {
            return new ReadinessResult(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), null, totalQuestionsAnswered);
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedMasterySum = BigDecimal.ZERO;

        for (TopicWeightAndMastery t : activeTopics) {
            BigDecimal w = (t.weight() != null) ? t.weight() : BigDecimal.ONE;
            BigDecimal m = (t.masteryPercent() != null) ? t.masteryPercent() : BigDecimal.ZERO;
            totalWeight = totalWeight.add(w);
            weightedMasterySum = weightedMasterySum.add(w.multiply(m));
        }

        BigDecimal trackMastery = (totalWeight.compareTo(BigDecimal.ZERO) > 0)
                ? weightedMasterySum.divide(totalWeight, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal readiness;
        if (totalEverQueuedMistakesCount >= minMistakesForClearance) {
            BigDecimal clearanceRatio = BigDecimal.valueOf(graduatedMistakesCount)
                    .divide(BigDecimal.valueOf(totalEverQueuedMistakesCount), 4, RoundingMode.HALF_UP);
            BigDecimal clearancePercent = clearanceRatio.multiply(BigDecimal.valueOf(100));

            readiness = trackMastery.multiply(BigDecimal.valueOf(0.8))
                    .add(clearancePercent.multiply(BigDecimal.valueOf(0.2)));
        } else {
            readiness = trackMastery;
        }

        return new ReadinessResult(readiness.setScale(1, RoundingMode.HALF_UP), null, totalQuestionsAnswered);
    }
}
