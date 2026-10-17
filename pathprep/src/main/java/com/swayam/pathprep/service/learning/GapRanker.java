package com.swayam.pathprep.service.learning;

import com.swayam.pathprep.enums.GapKind;
import com.swayam.pathprep.enums.SkillStatus;
import com.swayam.pathprep.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

public class GapRanker {

    public record Candidate(
            Long topicId,
            String topicName,
            BigDecimal weight,
            BigDecimal masteryPercent,
            int publishedQuestionCount,
            SkillStatus status,
            int attemptsCount,
            int correctCount,
            boolean prerequisitesMet
    ) {}

    public record RankedGap(
            Long topicId,
            String topicName,
            GapKind kind,
            BigDecimal masteryPercent,
            BigDecimal weight,
            BigDecimal impactScore,
            String reason,
            SuggestedAction suggestedAction
    ) {}

    public record SuggestedAction(
            String type,
            Long topicId,
            int questionCount
    ) {}

    public List<RankedGap> rank(List<Candidate> candidates, Integer limitParam) {
        int limit = (limitParam == null) ? 5 : limitParam;
        if (limit < 1 || limit > 10) {
            throw new BadRequestException("INVALID_PARAMETER", "Limit must be between 1 and 10");
        }

        if (candidates == null) {
            return List.of();
        }

        return candidates.stream()
                .filter(c -> c.publishedQuestionCount() >= 1)
                .filter(c -> c.status() != SkillStatus.MASTERED)
                .map(c -> {
                    BigDecimal m = (c.masteryPercent() != null) ? c.masteryPercent() : BigDecimal.ZERO;
                    BigDecimal w = (c.weight() != null) ? c.weight() : BigDecimal.ONE;
                    BigDecimal gap = BigDecimal.valueOf(100).subtract(m);
                    BigDecimal impact = w.multiply(gap).setScale(1, RoundingMode.HALF_UP);

                    GapKind kind;
                    String reason;
                    if (!c.prerequisitesMet()) {
                        kind = GapKind.PREREQUISITE_BLOCKED;
                        reason = "Weight " + w.stripTrailingZeros().toPlainString() + ", prerequisites unmet (advisory)";
                    } else if (c.attemptsCount() == 0) {
                        kind = GapKind.NOT_STARTED;
                        reason = "Weight " + w.stripTrailingZeros().toPlainString() + ", not started yet";
                    } else {
                        kind = GapKind.WEAK;
                        reason = "Weight " + w.stripTrailingZeros().toPlainString() + ", you answered " + c.correctCount() + " of " + c.attemptsCount() + " correctly";
                    }

                    SuggestedAction action = new SuggestedAction("DRILL", c.topicId(), 5);
                    return new RankedGap(
                            c.topicId(),
                            c.topicName(),
                            kind,
                            c.masteryPercent(),
                            w,
                            impact,
                            reason,
                            action
                    );
                })
                .sorted(Comparator.comparing(RankedGap::impactScore, Comparator.reverseOrder())
                        .thenComparing(RankedGap::weight, Comparator.reverseOrder())
                        .thenComparing(RankedGap::topicName)
                        .thenComparing(RankedGap::topicId))
                .limit(limit)
                .toList();
    }
}
