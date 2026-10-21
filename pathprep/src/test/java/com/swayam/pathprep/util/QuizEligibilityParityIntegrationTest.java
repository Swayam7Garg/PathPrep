package com.swayam.pathprep.util;

import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.QuestionOption;
import com.swayam.pathprep.entity.Topic;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;
import com.swayam.pathprep.repository.QuestionRepository;
import com.swayam.pathprep.repository.TopicRepository;
import com.swayam.pathprep.repository.projection.QuestionCandidateRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuizEligibilityParityIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void parity_sqlAndJavaProduceIdenticalEligibleQuestionSets() {
        Topic topic = topicRepository.findById(1L).orElseThrow();
        List<Long> testQuestionIds = new ArrayList<>();

        // 1. Eligible: PUBLISHED MCQ, 2 options, 1 correct
        testQuestionIds.add(createQuestion(topic, QuestionStatus.PUBLISHED, QuestionType.MCQ, 2, 1));

        // 2. Eligible: PUBLISHED MCQ, 4 options, 1 correct
        testQuestionIds.add(createQuestion(topic, QuestionStatus.PUBLISHED, QuestionType.MCQ, 4, 1));

        // 3. Ineligible: DRAFT MCQ, 4 options, 1 correct
        testQuestionIds.add(createQuestion(topic, QuestionStatus.DRAFT, QuestionType.MCQ, 4, 1));

        // 4. Ineligible: FLAGGED MCQ, 4 options, 1 correct
        testQuestionIds.add(createQuestion(topic, QuestionStatus.FLAGGED, QuestionType.MCQ, 4, 1));

        // 5. Ineligible: ARCHIVED MCQ, 4 options, 1 correct
        testQuestionIds.add(createQuestion(topic, QuestionStatus.ARCHIVED, QuestionType.MCQ, 4, 1));

        // 6. Ineligible: PUBLISHED MCQ, 1 option, 1 correct (<2 options)
        testQuestionIds.add(createQuestion(topic, QuestionStatus.PUBLISHED, QuestionType.MCQ, 1, 1));

        // 7. Ineligible: PUBLISHED MCQ, 4 options, 0 correct
        testQuestionIds.add(createQuestion(topic, QuestionStatus.PUBLISHED, QuestionType.MCQ, 4, 0));

        // 8. Ineligible: PUBLISHED MCQ, 4 options, 2 correct (>1 correct)
        testQuestionIds.add(createQuestion(topic, QuestionStatus.PUBLISHED, QuestionType.MCQ, 4, 2));

        // --- 1. Evaluate using Java QuizEligibility ---
        List<Question> loadedQuestions = questionRepository.findWithDetailsByIdIn(testQuestionIds);
        Set<Long> javaEligibleIds = loadedQuestions.stream()
                .filter(QuizEligibility::isEligible)
                .map(Question::getId)
                .collect(Collectors.toSet());

        // --- 2. Evaluate using SQL candidate query ---
        List<QuestionCandidateRow> sqlRows = questionRepository.findCandidateQuestions(List.of(topic.getId()), 0);
        Set<Long> sqlEligibleIds = sqlRows.stream()
                .map(QuestionCandidateRow::getId)
                .filter(testQuestionIds::contains)
                .collect(Collectors.toSet());

        // --- 3. Parity Assertion: Must be 100% identical ---
        assertThat(sqlEligibleIds)
                .as("SQL query and Java QuizEligibility rule must produce the exact same eligible question set")
                .isEqualTo(javaEligibleIds);

        // Ensure exactly the 2 valid questions are eligible
        assertThat(javaEligibleIds).hasSize(2);
    }

    private Long createQuestion(Topic topic, QuestionStatus status, QuestionType type, int totalOptions, int correctCount) {
        Question q = new Question();
        q.setTopic(topic);
        q.setType(type);
        q.setDifficulty(1);
        q.setBody("Parity test question " + UUID.randomUUID());
        q.setStatus(status);

        int assignedCorrect = 0;
        for (int i = 0; i < totalOptions; i++) {
            boolean isCorrect = assignedCorrect < correctCount;
            if (isCorrect) assignedCorrect++;
            q.getOptions().add(new QuestionOption(null, q, (byte) i, "Option " + i, isCorrect, null));
        }

        Question saved = questionRepository.save(q);
        return saved.getId();
    }
}
