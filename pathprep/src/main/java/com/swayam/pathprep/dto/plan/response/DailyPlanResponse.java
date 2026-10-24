package com.swayam.pathprep.dto.plan.response;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanResponse(
        LocalDate date,
        int totalItems,
        int completedItems,
        int progressPercent,
        boolean hasUnfinishedYesterdayTasks,
        List<DailyPlanItemResponse> items
) {
}
