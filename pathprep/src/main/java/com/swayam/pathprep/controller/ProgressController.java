package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.learning.ProgressResponse;
import com.swayam.pathprep.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/me/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping
    public ResponseEntity<ProgressResponse> getProgress(@RequestParam(required = false) Integer range) {
        log.info("Received request for user progress: range={}", range);
        ProgressResponse response = progressService.getProgress(range);
        return ResponseEntity.ok(response);
    }
}
