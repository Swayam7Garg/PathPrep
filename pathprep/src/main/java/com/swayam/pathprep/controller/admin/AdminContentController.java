package com.swayam.pathprep.controller.admin;

import com.swayam.pathprep.dto.admin.response.CoverageResponse;
import com.swayam.pathprep.service.admin.CoverageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final CoverageService coverageService;

    @GetMapping("/coverage")
    public ResponseEntity<List<CoverageResponse>> getCoverage(
            @RequestParam(required = false) Long trackId,
            @RequestParam(required = false, defaultValue = "10") Integer target,
            @RequestParam(required = false, defaultValue = "false") boolean includeSubtopics
    ) {
        log.info("Admin querying content coverage: trackId={}, target={}, includeSubtopics={}",
                trackId, target, includeSubtopics);
        List<CoverageResponse> coverage = coverageService.getCoverage(trackId, target, includeSubtopics);
        return ResponseEntity.ok(coverage);
    }
}
