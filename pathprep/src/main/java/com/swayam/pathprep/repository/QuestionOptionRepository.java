package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for querying answer options associated with questions.
 */
@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
}
