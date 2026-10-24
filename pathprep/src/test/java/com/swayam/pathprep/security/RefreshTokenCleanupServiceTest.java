package com.swayam.pathprep.security;

import com.swayam.pathprep.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RefreshTokenCleanupServiceTest {

    @Test
    void cleanupExpiredTokens_deletesTokensExpiredMoreThan7DaysAgo() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        Instant now = Instant.parse("2026-10-23T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneId.of("UTC"));

        Instant expectedCutoff = now.minus(7, ChronoUnit.DAYS);
        when(repository.deleteByExpiresAtBefore(expectedCutoff)).thenReturn(5);

        RefreshTokenCleanupService service = new RefreshTokenCleanupService(repository, clock);
        int deleted = service.cleanupExpiredTokens();

        assertThat(deleted).isEqualTo(5);
        verify(repository, times(1)).deleteByExpiresAtBefore(expectedCutoff);
    }
}
