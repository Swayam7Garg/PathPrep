package com.swayam.pathprep.util;

import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.QuestionOption;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;

/**
 * Single source of truth for the quiz-eligibility rule.
 *
 * <p>A question is quiz-eligible (i.e. can be selected for new sessions, counted in
 * track/topic stats, and offered in the review queue) when ALL of the following hold:
 * <ol>
 *   <li>status = PUBLISHED</li>
 *   <li>type = MCQ</li>
 *   <li>has at least 2 options</li>
 *   <li>has exactly 1 correct option</li>
 * </ol>
 *
 * <p>This class is used by:
 * <ul>
 *   <li>{@link com.swayam.pathprep.service.QuestionSelector} – candidate pool query</li>
 *   <li>{@link com.swayam.pathprep.repository.RoleTrackRepository} – track question counts</li>
 *   <li>{@link com.swayam.pathprep.repository.TrackTopicRepository} – topic question counts</li>
 *   <li>{@link com.swayam.pathprep.repository.ReviewQueueRepository} – review due list</li>
 *   <li>{@link com.swayam.pathprep.service.admin.AdminQuestionService} – publish validation</li>
 *   <li>{@link com.swayam.pathprep.service.admin.CoverageService} – coverage report</li>
 * </ul>
 *
 * <p>The inline SQL fragment {@link #SQL_CONDITION} must stay in sync with {@link #isEligible(Question)}.
 * If you change the rule, update BOTH.
 */
public final class QuizEligibility {

    private QuizEligibility() {}

    /**
     * Inline SQL fragment (to be embedded into native queries) that filters for quiz-eligible questions.
     * References the {@code questions} table aliased as {@code q} and {@code question_options} as {@code qo}.
     */
    public static final String SQL_CONDITION = """
            q.status = 'PUBLISHED'
            AND q.type = 'MCQ'
            AND (SELECT COUNT(*) FROM question_options qo WHERE qo.question_id = q.id) >= 2
            AND (SELECT COUNT(*) FROM question_options qo WHERE qo.question_id = q.id AND qo.is_correct = 1) = 1
            """;

    /**
     * Returns {@code true} if the given question is quiz-eligible.
     * The question must have its {@code options} collection loaded (not lazy-proxied).
     *
     * @param q the question to test
     * @return {@code true} if it meets all eligibility criteria
     */
    public static boolean isEligible(Question q) {
        if (q.getStatus() != QuestionStatus.PUBLISHED) return false;
        if (q.getType() != QuestionType.MCQ) return false;
        var opts = q.getOptions();
        if (opts == null || opts.size() < 2) return false;
        long correctCount = opts.stream().filter(QuestionOption::isCorrect).count();
        return correctCount == 1;
    }

    /**
     * Returns a human-readable list of reasons why {@code q} is not quiz-eligible.
     * Returns an empty list if the question passes all checks.
     *
     * @param q the question to validate (options must be loaded)
     * @return list of violation descriptions; empty means valid
     */
    public static java.util.List<String> violations(Question q) {
        var errors = new java.util.ArrayList<String>();
        if (q.getStatus() != QuestionStatus.PUBLISHED) {
            errors.add("status must be PUBLISHED (current: " + q.getStatus() + ")");
        }
        if (q.getType() != QuestionType.MCQ) {
            errors.add("type must be MCQ (current: " + q.getType() + ")");
        }
        var opts = q.getOptions();
        int optCount = opts == null ? 0 : opts.size();
        if (optCount < 2) {
            errors.add("requires at least 2 options (current: " + optCount + ")");
        }
        if (opts != null) {
            long correct = opts.stream().filter(QuestionOption::isCorrect).count();
            if (correct == 0) errors.add("requires exactly 1 correct option (found 0)");
            else if (correct > 1) errors.add("requires exactly 1 correct option (found " + correct + ")");
        }
        return errors;
    }
}
