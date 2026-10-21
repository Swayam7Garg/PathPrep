package com.swayam.pathprep.util;

import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.QuestionOption;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizEligibilityTest {

    @Test
    void isEligible_returnsTrue_whenAllCriteriaMet() {
        Question q = new Question();
        q.setStatus(QuestionStatus.PUBLISHED);
        q.setType(QuestionType.MCQ);
        q.getOptions().add(new QuestionOption(1L, q, 0, "A", true, null));
        q.getOptions().add(new QuestionOption(2L, q, 1, "B", false, null));

        assertThat(QuizEligibility.isEligible(q)).isTrue();
        assertThat(QuizEligibility.violations(q)).isEmpty();
    }

    @Test
    void isEligible_returnsFalse_whenDraftOrFlaggedOrArchived() {
        Question q = new Question();
        q.setStatus(QuestionStatus.DRAFT);
        q.setType(QuestionType.MCQ);
        q.getOptions().add(new QuestionOption(1L, q, 0, "A", true, null));
        q.getOptions().add(new QuestionOption(2L, q, 1, "B", false, null));

        assertThat(QuizEligibility.isEligible(q)).isFalse();
        assertThat(QuizEligibility.violations(q)).contains("status must be PUBLISHED (current: DRAFT)");

        q.setStatus(QuestionStatus.FLAGGED);
        assertThat(QuizEligibility.isEligible(q)).isFalse();

        q.setStatus(QuestionStatus.ARCHIVED);
        assertThat(QuizEligibility.isEligible(q)).isFalse();
    }

    @Test
    void isEligible_returnsFalse_whenFewerThanTwoOptions() {
        Question q = new Question();
        q.setStatus(QuestionStatus.PUBLISHED);
        q.setType(QuestionType.MCQ);
        q.getOptions().add(new QuestionOption(1L, q, 0, "A", true, null));

        assertThat(QuizEligibility.isEligible(q)).isFalse();
        assertThat(QuizEligibility.violations(q)).contains("requires at least 2 options (current: 1)");
    }

    @Test
    void isEligible_returnsFalse_whenZeroOrMultipleCorrectOptions() {
        Question q = new Question();
        q.setStatus(QuestionStatus.PUBLISHED);
        q.setType(QuestionType.MCQ);
        q.getOptions().add(new QuestionOption(1L, q, 0, "A", false, null));
        q.getOptions().add(new QuestionOption(2L, q, 1, "B", false, null));

        assertThat(QuizEligibility.isEligible(q)).isFalse();
        assertThat(QuizEligibility.violations(q)).contains("requires exactly 1 correct option (found 0)");

        q.getOptions().get(0).setCorrect(true);
        q.getOptions().get(1).setCorrect(true);
        assertThat(QuizEligibility.isEligible(q)).isFalse();
        assertThat(QuizEligibility.violations(q)).contains("requires exactly 1 correct option (found 2)");
    }
}
