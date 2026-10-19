package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.SkillMastery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for querying and updating student topic mastery scores and proficiency tiers.
 */
@Repository
public interface SkillMasteryRepository extends JpaRepository<SkillMastery, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sm FROM SkillMastery sm WHERE sm.user.id = :userId AND sm.topic.id = :topicId")
    Optional<SkillMastery> findByUserIdAndTopicIdWithLock(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Query("SELECT sm FROM SkillMastery sm WHERE sm.user.id = :userId AND sm.topic.id = :topicId")
    Optional<SkillMastery> findByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Query("SELECT sm FROM SkillMastery sm JOIN FETCH sm.topic WHERE sm.user.id = :userId AND sm.topic.id IN :topicIds")
    List<SkillMastery> findByUserIdAndTopicIdIn(@Param("userId") Long userId, @Param("topicIds") Collection<Long> topicIds);

    @Query("SELECT sm FROM SkillMastery sm JOIN FETCH sm.topic WHERE sm.user.id = :userId")
    List<SkillMastery> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(sm.attemptsCount), 0) FROM SkillMastery sm WHERE sm.user.id = :userId")
    int sumAttemptsCountByUserId(@Param("userId") Long userId);
}
