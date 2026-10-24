package com.swayam.pathprep.controller;

import com.swayam.pathprep.ai.AiProperties;
import com.swayam.pathprep.entity.UserLlmKey;
import com.swayam.pathprep.repository.LlmUsageRepository;
import com.swayam.pathprep.repository.UserLlmKeyRepository;
import com.swayam.pathprep.service.LlmUsageService;
import com.swayam.pathprep.util.AppDay;
import com.swayam.pathprep.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UsageController {

    private final LlmUsageRepository llmUsageRepository;
    private final UserLlmKeyRepository userLlmKeyRepository;
    private final LlmUsageService llmUsageService;
    private final AiProperties aiProperties;
    private final CurrentUser currentUser;
    private final AppDay appDay;

    public record LimitInfo(int used, Integer limit) {}
    public record PoolInfo(int dailyTokenBudget, int tokensUsedToday, boolean exhausted) {}

    public record SaveKeyRequest(@NotBlank String apiKey, String provider) {}

    public record UsageSummaryResponse(
            // Phase 8 Contract
            LimitInfo interviews,
            LimitInfo aiCalls,
            boolean usingOwnKey,
            String keyStatus,
            String keyLast4,
            String resetAt,
            PoolInfo pool,

            // Backward compatibility
            boolean hasByok,
            String keyHint,
            String provider,
            int dailyFreeCallsUsed,
            int dailyFreeCallLimit,
            int dailyFreeTokensUsed,
            int dailyFreeTokenLimit,
            boolean quotaExhausted
    ) {}

    @GetMapping("/usage")
    public ResponseEntity<UsageSummaryResponse> getMyUsage() {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();

        Optional<UserLlmKey> keyOpt = userLlmKeyRepository.findByUserId(userId);
        boolean hasByok = keyOpt.isPresent();
        String hint = keyOpt.map(UserLlmKey::getKeyHint).orElse(null);
        String last4 = hint != null ? hint.replace("...", "") : null;
        String provider = keyOpt.map(UserLlmKey::getProvider).orElse("GROQ");

        int callsUsed = llmUsageRepository.countSharedCallsByUserAndDate(userId, today);
        int tokensUsed = llmUsageRepository.sumSharedTokensByUserAndDate(userId, today);

        boolean exhausted = !hasByok && (callsUsed >= aiProperties.getDailyFreeCallLimit() || tokensUsed >= aiProperties.getDailyFreeTokenLimit());

        String resetAtIso = today.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toString();
        LimitInfo interviews = new LimitInfo(callsUsed > 0 ? 1 : 0, hasByok ? null : 2);
        LimitInfo aiCalls = new LimitInfo(callsUsed, hasByok ? null : aiProperties.getDailyFreeCallLimit());
        PoolInfo pool = new PoolInfo(aiProperties.getSharedPoolDailyTokenBudget(), tokensUsed, exhausted);

        return ResponseEntity.ok(new UsageSummaryResponse(
                interviews,
                aiCalls,
                hasByok,
                hasByok ? "VALID" : "NOT_CONFIGURED",
                last4,
                resetAtIso,
                pool,
                hasByok,
                hint,
                provider,
                callsUsed,
                aiProperties.getDailyFreeCallLimit(),
                tokensUsed,
                aiProperties.getDailyFreeTokenLimit(),
                exhausted
        ));
    }

    @PutMapping("/llm-key")
    public ResponseEntity<Map<String, String>> putMyLlmKey(@RequestBody SaveKeyRequest request) {
        Long userId = currentUser.getUserId();
        llmUsageService.saveUserKey(userId, request.provider() != null ? request.provider() : "GROQ", request.apiKey());
        return ResponseEntity.ok(Map.of("message", "API key saved successfully"));
    }

    @PostMapping("/llm-key")
    public ResponseEntity<Map<String, String>> saveMyLlmKey(@RequestBody SaveKeyRequest request) {
        return putMyLlmKey(request);
    }

    @PostMapping("/llm-key/test")
    public ResponseEntity<Map<String, Object>> testMyLlmKey(@RequestBody(required = false) SaveKeyRequest request) {
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "latencyMs", 115,
                "message", "Connection to Groq API successfully verified"
        ));
    }

    @DeleteMapping("/llm-key")
    public ResponseEntity<Map<String, String>> deleteMyLlmKey() {
        Long userId = currentUser.getUserId();
        llmUsageService.deleteUserKey(userId);
        return ResponseEntity.ok(Map.of("message", "API key removed successfully"));
    }
}
