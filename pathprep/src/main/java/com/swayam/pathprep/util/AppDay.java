package com.swayam.pathprep.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class AppDay {

    private final Clock clock;
    private final ZoneId zoneId;

    public AppDay(Clock clock, @Value("${app.time-zone:Asia/Kolkata}") String timeZone) {
        this.clock = clock;
        this.zoneId = ZoneId.of(timeZone);
    }

    public LocalDate today() {
        return clock.instant().atZone(zoneId).toLocalDate();
    }

    public ZoneId getZoneId() {
        return zoneId;
    }
}
