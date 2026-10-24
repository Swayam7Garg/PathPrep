package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.CriterionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterionScoreRepository extends JpaRepository<CriterionScore, Long> {
    List<CriterionScore> findByEvaluationId(Long evaluationId);
}
