package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.learning.response.TodayPlanResponse;
import com.swayam.pathprep.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/me/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/today")
    public ResponseEntity<TodayPlanResponse> getTodayPlan() {
        log.info("Received request for today's plan");
        TodayPlanResponse response = planService.getTodayPlan();
        return ResponseEntity.ok(response);
    }
}
