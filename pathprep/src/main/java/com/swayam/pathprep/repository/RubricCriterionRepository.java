package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.RubricCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, Long> {
    List<RubricCriterion> findByRubricId(Long rubricId);
}
