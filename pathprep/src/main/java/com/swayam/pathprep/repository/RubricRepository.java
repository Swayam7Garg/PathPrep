package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RubricRepository extends JpaRepository<Rubric, Long> {

    @Query("SELECT r FROM Rubric r LEFT JOIN FETCH r.criteria WHERE r.id = :id")
    Optional<Rubric> findWithCriteriaById(@Param("id") Long id);
}
