package com.swayam.pathprep.repository;

import com.swayam.pathprep.entity.LlmUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LlmUsageRepository extends JpaRepository<LlmUsage, Long> {

    Optional<LlmUsage> findByUserIdAndUsageDateAndProviderAndModelAndByok(
            Long userId, LocalDate usageDate, String provider, String model, boolean byok);

    @Query("SELECT COALESCE(SUM(u.callCount), 0) FROM LlmUsage u WHERE u.user.id = :userId AND u.usageDate = :usageDate AND u.byok = false")
    int countSharedCallsByUserAndDate(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate);

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM LlmUsage u WHERE u.user.id = :userId AND u.usageDate = :usageDate AND u.byok = false")
    int sumSharedTokensByUserAndDate(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate);

    @Query("SELECT u FROM LlmUsage u WHERE u.user.id = :userId AND u.usageDate >= :startDate ORDER BY u.usageDate DESC")
    List<LlmUsage> findRecentUsageByUser(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    @Query("SELECT u FROM LlmUsage u WHERE u.usageDate = :usageDate ORDER BY u.totalTokens DESC")
    List<LlmUsage> findAllUsageForDate(@Param("usageDate") LocalDate usageDate);
}
