package com.swayam.pathprep.controller.admin;

import com.swayam.pathprep.dto.response.QuestionReportResponse;
import com.swayam.pathprep.service.QuestionReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final QuestionReportService questionReportService;

    @GetMapping
    public ResponseEntity<Page<QuestionReportResponse>> listReports(
            @RequestParam(required = false) Long questionId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Admin listing question reports: questionId={}", questionId);
        Page<QuestionReportResponse> reports = questionReportService.listReports(questionId, pageable);
        return ResponseEntity.ok(reports);
    }
}
