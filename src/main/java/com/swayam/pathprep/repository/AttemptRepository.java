package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Attempt} entities (Phase 4).
 *
 * <p>Key queries:
 * <ul>
 *   <li>{@link #findByIdempotencyKey} — used at the very start of answer submission to
 *       detect duplicate requests before any state mutation.</li>
 * </ul>
 */
@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    /**
     * Looks up a previously stored attempt by idempotency key.
     *
     * <p>Called before any mutation so that network retries or double-clicks return
     * the same response without re-grading or double-inserting.
     *
     * @param idempotencyKey the client-supplied UUID
     * @return the existing {@link Attempt} if this key was already processed
     */
    Optional<Attempt> findByIdempotencyKey(String idempotencyKey);
}
