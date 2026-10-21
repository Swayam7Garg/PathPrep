package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.QuestionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionReportRepository extends JpaRepository<QuestionReport, Long> {

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<QuestionReport> findByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("SELECT r FROM QuestionReport r JOIN FETCH r.question q JOIN FETCH r.user u ORDER BY r.createdAt DESC")
    Page<QuestionReport> findAllWithDetails(Pageable pageable);

    @Query("SELECT r FROM QuestionReport r JOIN FETCH r.question q JOIN FETCH r.user u WHERE q.id = :questionId ORDER BY r.createdAt DESC")
    Page<QuestionReport> findByQuestionId(@Param("questionId") Long questionId, Pageable pageable);
}
