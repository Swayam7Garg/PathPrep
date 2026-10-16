package com.swayam.pathprep.service;

import java.math.BigDecimal;

public final class TopicPerformanceEvaluator {

    public static final int MIN_ANSWERED_QUESTIONS = 2;
    public static final double STRONG_THRESHOLD_PERCENT = 80.0;
    public static final double WEAK_THRESHOLD_PERCENT = 50.0;

    public static final String LABEL_NOT_ENOUGH_DATA = "NOT_ENOUGH_DATA";
    public static final String LABEL_STRONG = "STRONG";
    public static final String LABEL_OK = "OK";
    public static final String LABEL_WEAK = "WEAK";

    private TopicPerformanceEvaluator() {}

    public static String evaluateLabel(int answeredCount, BigDecimal accuracyPercent) {
        if (answeredCount < MIN_ANSWERED_QUESTIONS) {
            return LABEL_NOT_ENOUGH_DATA;
        }
        double accuracy = accuracyPercent != null ? accuracyPercent.doubleValue() : 0.0;
        if (accuracy >= STRONG_THRESHOLD_PERCENT) {
            return LABEL_STRONG;
        } else if (accuracy < WEAK_THRESHOLD_PERCENT) {
            return LABEL_WEAK;
        } else {
            return LABEL_OK;
        }
    }
}
