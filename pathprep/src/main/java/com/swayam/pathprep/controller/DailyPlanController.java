package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.learning.response.TodayPlanResponse;
import com.swayam.pathprep.dto.plan.request.CreateDailyPlanItemRequest;
import com.swayam.pathprep.dto.plan.response.DailyPlanItemResponse;
import com.swayam.pathprep.dto.plan.response.DailyPlanResponse;
import com.swayam.pathprep.service.DailyPlanService;
import com.swayam.pathprep.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/me/plan")
@RequiredArgsConstructor
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;
    private final PlanService planService;

    /**
     * Returns today's checklist items, summary statistics, and whether unfinished yesterday tasks exist.
     */
    @GetMapping
    public ResponseEntity<DailyPlanResponse> getTodayDailyPlan() {
        log.info("Received request for today's user-defined daily plan");
        DailyPlanResponse response = dailyPlanService.getTodayPlan();
        return ResponseEntity.ok(response);
    }

    /**
     * Backward-compatible Phase 6 endpoint for algorithmically calculated daily recommendations (RESUME, REVIEW, DRILL).
     */
    @GetMapping("/today")
    public ResponseEntity<TodayPlanResponse> getTodayAlgorithmicPlan() {
        log.info("Received request for today's algorithmic recommendations plan");
        TodayPlanResponse response = planService.getTodayPlan();
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a new task to today's daily plan (either CUSTOM or QUIZ).
     */
    @PostMapping("/items")
    public ResponseEntity<DailyPlanItemResponse> addItem(@Valid @RequestBody CreateDailyPlanItemRequest request) {
        log.info("Received request to add daily plan item: type={}", request.itemType());
        DailyPlanItemResponse response = dailyPlanService.addItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Toggles the completion status of a daily plan item.
     */
    @PatchMapping("/items/{id}/toggle")
    public ResponseEntity<DailyPlanItemResponse> toggleItem(@PathVariable("id") Long id) {
        log.info("Received request to toggle daily plan item id={}", id);
        DailyPlanItemResponse response = dailyPlanService.toggleItem(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an item from today's daily plan.
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable("id") Long id) {
        log.info("Received request to delete daily plan item id={}", id);
        dailyPlanService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Rollover unfinished tasks from yesterday into today's plan.
     */
    @PostMapping("/rollover")
    public ResponseEntity<DailyPlanResponse> rolloverYesterdayTasks() {
        log.info("Received request to rollover yesterday's unfinished tasks");
        DailyPlanResponse response = dailyPlanService.rolloverYesterdayTasks();
        return ResponseEntity.ok(response);
    }
}
