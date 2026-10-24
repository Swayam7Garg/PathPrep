package com.swayam.pathprep.service;

import com.swayam.pathprep.dto.request.DeleteUserRequest;
import com.swayam.pathprep.dto.request.UpdateUserRequest;
import com.swayam.pathprep.dto.response.UserProfile;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.RoleTrackRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeService {

    /**
     * Set of database tables that have a foreign key to users table.
     * Verified by information_schema regression tests to prevent unhandled tables.
     */
    public static final Set<String> HANDLED_USER_FK_TABLES = Set.of(
            "review_attempts",
            "review_queue",
            "daily_activity",
            "skill_mastery",
            "attempts",
            "session_questions",
            "sessions",
            "question_reports",
            "cohort_members",
            "refresh_tokens",
            "daily_plan_items"
    );

    private final UserRepository userRepository;
    private final RoleTrackRepository roleTrackRepository;
    private final UserProfileAssembler userProfileAssembler;
    private final CurrentUser currentUser;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public UserProfile getCurrentUserProfile() {
        Long userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found with id: " + userId));
        return userProfileAssembler.toProfile(user);
    }

    @Transactional
    public UserProfile updateProfile(UpdateUserRequest request) {
        Long userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found with id: " + userId));

        if (request.name() != null) {
            String trimmedName = request.name().trim();
            if (trimmedName.isBlank() || trimmedName.length() > 100) {
                throw new BadRequestException("INVALID_NAME", "Name must be non-blank and at most 100 characters");
            }
            user.setName(trimmedName);
        }

        if (request.branch() != null) {
            String trimmedBranch = request.branch().trim();
            if (trimmedBranch.isBlank() || trimmedBranch.length() > 50) {
                throw new BadRequestException("INVALID_BRANCH", "Branch must be at most 50 characters");
            }
            user.setBranch(trimmedBranch);
        }

        if (request.gradYear() != null) {
            int currentYear = LocalDate.now(clock).getYear();
            int minYear = currentYear - 1;
            int maxYear = currentYear + 6;
            if (request.gradYear() < minYear || request.gradYear() > maxYear) {
                throw new BadRequestException("INVALID_GRAD_YEAR",
                        "Graduation year must be between " + minYear + " and " + maxYear);
            }
            user.setGradYear(request.gradYear());
        }

        if (request.targetTrackId() != null) {
            boolean exists = roleTrackRepository.existsById(request.targetTrackId());
            if (!exists) {
                throw new NotFoundException("TRACK_NOT_FOUND", "Role track not found with id: " + request.targetTrackId());
            }
            user.setTargetTrackId(request.targetTrackId());
        }

        user.setUpdatedAt(clock.instant());
        User savedUser = userRepository.save(user);
        return userProfileAssembler.toProfile(savedUser);
    }

    @Transactional
    public void deleteAccount(DeleteUserRequest request) {
        if (!"DELETE".equals(request.confirm())) {
            throw new BadRequestException("VALIDATION_FAILED", "Confirmation must equal 'DELETE'");
        }

        Long userId = currentUser.getUserId();
        log.warn("Executing account hard-deletion for user id: {}", userId);

        // Delete all dependent child rows in reverse dependency order
        entityManager.createNativeQuery("DELETE FROM review_attempts WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM review_queue WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM daily_activity WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM skill_mastery WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM attempts WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM daily_plan_items WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM session_questions WHERE session_id IN (SELECT id FROM sessions WHERE user_id = :userId)")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM sessions WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM question_reports WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM cohort_members WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM refresh_tokens WHERE user_id = :userId")
                .setParameter("userId", userId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM users WHERE id = :userId")
                .setParameter("userId", userId).executeUpdate();

        log.info("Account deletion completed successfully for user id: {}", userId);
    }
}
