package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.ReviewAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for logging spaced repetition review outcomes and history.
 */
@Repository
public interface ReviewAttemptRepository extends JpaRepository<ReviewAttempt, Long> {
}
