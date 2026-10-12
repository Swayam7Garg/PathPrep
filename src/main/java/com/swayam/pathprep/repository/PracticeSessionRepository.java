package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.PracticeSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    /**
     * Loads a session owned by the given user with a row-level write lock.
     *
     * <p>Used during answer submission (Phase 4) to prevent two concurrent requests
     * from racing and double-incrementing {@code currentIndex}. The lock is held for
     * the lifetime of the enclosing {@code @Transactional} method.
     *
     * <p>Ownership is checked at the DB level ({@code AND user_id = :userId}) so that
     * a student cannot submit answers to another student's session; returns
     * {@link Optional#empty()} in that case, and the service layer throws a 404.
     *
     * @param id     session primary key
     * @param userId the authenticated user's ID
     * @return the locked session, or empty if not found or not owned by this user
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PracticeSession s WHERE s.id = :id AND s.user.id = :userId")
    Optional<PracticeSession> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);
}

