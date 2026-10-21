package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.request.CreateQuestionReportRequest;
import com.swayam.pathprep.dto.response.QuestionReportResponse;
import com.swayam.pathprep.service.QuestionReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/questions/{id}/report")
@RequiredArgsConstructor
public class QuestionReportController {

    private final QuestionReportService questionReportService;

    @PostMapping
    public ResponseEntity<QuestionReportResponse> reportQuestion(
            @PathVariable Long id,
            @Valid @RequestBody CreateQuestionReportRequest request
    ) {
        log.info("Student reporting question id={}, reason={}", id, request.reason());
        QuestionReportResponse response = questionReportService.reportQuestion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
