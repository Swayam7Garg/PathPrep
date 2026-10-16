package com.swayam.pathprep.enums;

public enum QuizDifficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3),
    MIXED(0);

    private final int level;

    QuizDifficulty(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
