package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.repository.projection.QuestionCandidateRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for fetching active questions, candidate pools for sessions, and topic-level question counts.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    boolean existsByContentHash(String contentHash);

    Optional<Question> findByContentHash(String contentHash);

    /**
     * Returns published MCQ questions that are quiz-eligible for the given topics and difficulty.
     * A question is eligible if it has status=PUBLISHED, type=MCQ, at least 2 options, and exactly 1 correct option.
     * This must stay in sync with {@link com.swayam.pathprep.util.QuizEligibility}.
     */
    @Query(value = """
        SELECT q.id AS id, q.topic_id AS topicId
        FROM questions q
        WHERE q.topic_id IN (:topicIds)
          AND q.type = 'MCQ'
          AND q.status = 'PUBLISHED'
          AND (:difficulty = 0 OR q.difficulty = :difficulty)
          AND (SELECT COUNT(*) FROM question_options qo WHERE qo.question_id = q.id) >= 2
          AND (SELECT COUNT(*) FROM question_options qo WHERE qo.question_id = q.id AND qo.is_correct = 1) = 1
        """, nativeQuery = true)
    List<QuestionCandidateRow> findCandidateQuestions(
            @Param("topicIds") Collection<Long> topicIds,
            @Param("difficulty") int difficulty
    );

    @Query(value = """
        SELECT q.id AS id, q.topic_id AS topicId
        FROM questions q
        WHERE q.topic_id IN (:topicIds)
          AND q.type IN ('CONCEPT', 'SCENARIO')
          AND q.status = 'PUBLISHED'
          AND (:difficulty = 0 OR q.difficulty = :difficulty)
        """, nativeQuery = true)
    List<QuestionCandidateRow> findCandidateInterviewQuestions(
            @Param("topicIds") Collection<Long> topicIds,
            @Param("difficulty") int difficulty
    );


    @Query("SELECT q FROM Question q JOIN FETCH q.topic LEFT JOIN FETCH q.options WHERE q.id = :id")
    Optional<Question> findWithDetailsById(@Param("id") Long id);

    @Query("SELECT DISTINCT q FROM Question q JOIN FETCH q.topic LEFT JOIN FETCH q.options WHERE q.id IN :ids")
    List<Question> findWithDetailsByIdIn(@Param("ids") Collection<Long> ids);
}
