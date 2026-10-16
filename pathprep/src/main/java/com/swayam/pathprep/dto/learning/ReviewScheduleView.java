package com.swayam.pathprep.dto.learning;

import java.time.LocalDate;

public record ReviewScheduleView(
        int step,
        Integer intervalDays,
        LocalDate nextDueOn,
        boolean graduated
) {}
