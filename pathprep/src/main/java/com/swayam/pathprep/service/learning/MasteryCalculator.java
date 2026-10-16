package com.swayam.pathprep.service.learning;

import com.swayam.pathprep.enums.ConfidenceLevel;
import com.swayam.pathprep.enums.SkillStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MasteryCalculator {

    private final int masteryWindow;
    private final int priorCorrect;
    private final int priorAttempts;
    private final int masteredThreshold;

    public MasteryCalculator(int masteryWindow, int priorCorrect, int priorAttempts, int masteredThreshold) {
        this.masteryWindow = masteryWindow;
        this.priorCorrect = priorCorrect;
        this.priorAttempts = priorAttempts;
        this.masteredThreshold = masteredThreshold;
    }

    public String appendOutcome(String currentRecent, boolean correct) {
        String base = (currentRecent == null) ? "" : currentRecent;
        String updated = base + (correct ? "1" : "0");
        if (updated.length() > masteryWindow) {
            updated = updated.substring(updated.length() - masteryWindow);
        }
        return updated;
    }

    public BigDecimal calculateScore(String recentOutcomes) {
        if (recentOutcomes == null || recentOutcomes.isEmpty()) {
            return null;
        }
        int correctInRecent = 0;
        for (int i = 0; i < recentOutcomes.length(); i++) {
            if (recentOutcomes.charAt(i) == '1') {
                correctInRecent++;
            }
        }
        int numerator = correctInRecent + priorCorrect;
        int denominator = recentOutcomes.length() + priorAttempts;
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    public ConfidenceLevel calculateConfidence(int lifetimeAttempts) {
        if (lifetimeAttempts < 3) {
            return ConfidenceLevel.LOW;
        } else if (lifetimeAttempts <= 7) {
            return ConfidenceLevel.MEDIUM;
        } else {
            return ConfidenceLevel.HIGH;
        }
    }

    public SkillStatus calculateStatus(BigDecimal masteryScore, ConfidenceLevel confidence) {
        if (masteryScore == null) {
            return SkillStatus.NOT_STARTED;
        }
        if (masteryScore.compareTo(BigDecimal.valueOf(masteredThreshold)) >= 0 && confidence != ConfidenceLevel.LOW) {
            return SkillStatus.MASTERED;
        }
        return SkillStatus.IN_PROGRESS;
    }
}
