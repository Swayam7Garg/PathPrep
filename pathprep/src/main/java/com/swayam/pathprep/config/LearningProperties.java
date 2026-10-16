package com.swayam.pathprep.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.learning")
public class LearningProperties {
    private int dailyGoal = 10;
    private int masteryWindow = 20;
    private int masteryPriorCorrect = 1;
    private int masteryPriorAttempts = 2;
    private int masteredThreshold = 80;
    private int prerequisiteThreshold = 60;
    private List<Integer> reviewIntervalsDays = List.of(1, 3, 7, 14);
    private int maxSecondsPerAnswer = 300;
    private int minAnswersForReadiness = 10;
    private int minMistakesForClearance = 5;
}
