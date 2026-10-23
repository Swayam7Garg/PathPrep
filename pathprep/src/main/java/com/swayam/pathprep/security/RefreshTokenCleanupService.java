package com.swayam.pathprep.security;

import com.swayam.pathprep.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    /**
     * Daily cleanup: deletes refresh tokens that expired more than 7 days ago.
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM every day
    @Transactional
    public int cleanupExpiredTokens() {
        Instant cutoff = clock.instant().minus(7, ChronoUnit.DAYS);
        log.info("Running scheduled refresh token cleanup. Removing tokens expired before {}", cutoff);
        int deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Expired refresh token cleanup completed. Deleted {} tokens.", deletedCount);
        return deletedCount;
    }
}
