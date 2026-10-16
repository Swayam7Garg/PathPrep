package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.learning.SkillGapsResponse;
import com.swayam.pathprep.dto.learning.SkillProfileResponse;
import com.swayam.pathprep.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/me/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<SkillProfileResponse> getSkills(@RequestParam(required = false) Long trackId) {
        log.info("Received request for user skill profile: trackId={}", trackId);
        SkillProfileResponse response = skillService.getSkillProfile(trackId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/gaps")
    public ResponseEntity<SkillGapsResponse> getSkillGaps(
            @RequestParam(required = false) Long trackId,
            @RequestParam(required = false) Integer limit
    ) {
        log.info("Received request for user skill gaps: trackId={}, limit={}", trackId, limit);
        SkillGapsResponse response = skillService.getSkillGaps(trackId, limit);
        return ResponseEntity.ok(response);
    }
}
