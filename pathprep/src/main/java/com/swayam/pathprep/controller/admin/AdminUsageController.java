package com.swayam.pathprep.controller.admin;

import com.swayam.pathprep.entity.LlmUsage;
import com.swayam.pathprep.repository.LlmUsageRepository;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminUsageController {

    private final LlmUsageRepository llmUsageRepository;
    private final AppDay appDay;

    public record AdminUsageSummary(
            LocalDate date,
            int totalCalls,
            int totalTokens,
            int byokCalls,
            int sharedCalls,
            List<UsageRow> records
    ) {}

    public record UsageRow(
            Long userId,
            String provider,
            String model,
            int callCount,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            boolean byok
    ) {}

    @GetMapping("/usage")
    public ResponseEntity<AdminUsageSummary> getAiUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : appDay.today();
        List<LlmUsage> list = llmUsageRepository.findAllUsageForDate(targetDate);

        int totalCalls = list.stream().mapToInt(LlmUsage::getCallCount).sum();
        int totalTokens = list.stream().mapToInt(LlmUsage::getTotalTokens).sum();
        int byokCalls = list.stream().filter(LlmUsage::isByok).mapToInt(LlmUsage::getCallCount).sum();
        int sharedCalls = totalCalls - byokCalls;

        List<UsageRow> rows = list.stream().map(u -> new UsageRow(
                u.getUser().getId(),
                u.getProvider(),
                u.getModel(),
                u.getCallCount(),
                u.getPromptTokens(),
                u.getCompletionTokens(),
                u.getTotalTokens(),
                u.isByok()
        )).toList();

        return ResponseEntity.ok(new AdminUsageSummary(
                targetDate,
                totalCalls,
                totalTokens,
                byokCalls,
                sharedCalls,
                rows
        ));
    }
}
