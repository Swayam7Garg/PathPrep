package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.DailyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for managing student-defined daily plan items and goals.
 */
@Repository
public interface DailyPlanItemRepository extends JpaRepository<DailyPlanItem, Long> {

    @Query("""
        SELECT dpi FROM DailyPlanItem dpi
        LEFT JOIN FETCH dpi.track
        LEFT JOIN FETCH dpi.topic
        LEFT JOIN FETCH dpi.session
        WHERE dpi.user.id = :userId AND dpi.planDate = :planDate
        ORDER BY dpi.sortOrder ASC, dpi.createdAt ASC
    """)
    List<DailyPlanItem> findByUserIdAndPlanDateWithDetails(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);

    @Query("""
        SELECT COUNT(dpi) > 0 FROM DailyPlanItem dpi
        WHERE dpi.user.id = :userId AND dpi.planDate = :planDate AND dpi.completed = false
    """)
    boolean hasUnfinishedTasksOnDate(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);

    @Query("""
        SELECT dpi FROM DailyPlanItem dpi
        LEFT JOIN FETCH dpi.track
        LEFT JOIN FETCH dpi.topic
        WHERE dpi.user.id = :userId AND dpi.planDate = :planDate AND dpi.completed = false
        ORDER BY dpi.sortOrder ASC, dpi.createdAt ASC
    """)
    List<DailyPlanItem> findUnfinishedItemsOnDate(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);

    @Query("SELECT dpi FROM DailyPlanItem dpi WHERE dpi.id = :id AND dpi.user.id = :userId")
    Optional<DailyPlanItem> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COALESCE(MAX(dpi.sortOrder), -1) FROM DailyPlanItem dpi WHERE dpi.user.id = :userId AND dpi.planDate = :planDate")
    int findMaxSortOrderByUserIdAndPlanDate(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);

    @Query("""
        SELECT dpi FROM DailyPlanItem dpi
        WHERE dpi.user.id = :userId
          AND dpi.planDate = :planDate
          AND dpi.itemType = com.swayam.pathprep.enums.DailyPlanItemType.QUIZ
          AND dpi.completed = false
          AND dpi.topic.id = :topicId
    """)
    List<DailyPlanItem> findOpenQuizItemsByTopic(@Param("userId") Long userId, @Param("planDate") LocalDate planDate, @Param("topicId") Long topicId);
}
