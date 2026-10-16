package com.swayam.pathprep.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AppDayTest {

    @Test
    void convertsInstantToKolkataDayCorrectly() {
        // 2026-10-15T18:35:00Z + 05:30 = 2026-10-16T00:05:00 -> 2026-10-16
        Clock clockAfterMidnight = Clock.fixed(Instant.parse("2026-10-15T18:35:00Z"), ZoneOffset.UTC);
        AppDay appDay1 = new AppDay(clockAfterMidnight, "Asia/Kolkata");
        assertThat(appDay1.today()).isEqualTo(LocalDate.of(2026, 10, 16));

        // 2026-10-15T18:25:00Z + 05:30 = 2026-10-15T23:55:00 -> 2026-10-15
        Clock clockBeforeMidnight = Clock.fixed(Instant.parse("2026-10-15T18:25:00Z"), ZoneOffset.UTC);
        AppDay appDay2 = new AppDay(clockBeforeMidnight, "Asia/Kolkata");
        assertThat(appDay2.today()).isEqualTo(LocalDate.of(2026, 10, 15));
    }
}
