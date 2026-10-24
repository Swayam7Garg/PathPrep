package com.swayam.pathprep.service;

import com.swayam.pathprep.ai.AiProperties;
import com.swayam.pathprep.ai.EncryptionService;
import com.swayam.pathprep.entity.LlmUsage;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.entity.UserLlmKey;
import com.swayam.pathprep.exception.ApiException;
import com.swayam.pathprep.repository.LlmUsageRepository;
import com.swayam.pathprep.repository.UserLlmKeyRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageService {

    private final LlmUsageRepository llmUsageRepository;
    private final UserLlmKeyRepository userLlmKeyRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final AiProperties aiProperties;
    private final AppDay appDay;

    /**
     * Resolves the API key to use for the user: BYOK decrypted key if registered, otherwise null (signals shared pool).
     */
    @Transactional(readOnly = true)
    public Optional<String> getUserApiKey(Long userId) {
        return userLlmKeyRepository.findByUserId(userId)
                .map(key -> encryptionService.decrypt(key.getEncryptedKey()));
    }

    /**
     * Checks whether user has remaining quota for AI execution.
     * Throws 429 AI_QUOTA_EXCEEDED if limits are reached on the shared pool.
     */
    @Transactional(readOnly = true)
    public void validateQuota(Long userId, boolean isByok) {
        if (isByok) {
            // BYOK users are unconstrained by the shared pool budget
            return;
        }

        LocalDate today = appDay.today();
        int userCalls = llmUsageRepository.countSharedCallsByUserAndDate(userId, today);
        if (userCalls >= aiProperties.getDailyFreeCallLimit()) {
            throw new com.swayam.pathprep.exception.QuotaExceededException(
                    "Daily free interview AI quota reached (" + aiProperties.getDailyFreeCallLimit() + " calls). Please provide your own Groq API key in Settings to continue practicing unlimited."
            );
        }

        int userTokens = llmUsageRepository.sumSharedTokensByUserAndDate(userId, today);
        if (userTokens >= aiProperties.getDailyFreeTokenLimit()) {
            throw new com.swayam.pathprep.exception.QuotaExceededException(
                    "Daily free AI token limit reached (" + aiProperties.getDailyFreeTokenLimit() + " tokens). Add your own Groq API key to continue."
            );
        }
    }

    /**
     * Records token and call consumption.
     */
    @Transactional
    public void recordUsage(Long userId, String provider, String model, int promptTokens, int completionTokens, boolean isByok) {
        LocalDate today = appDay.today();
        int totalTokens = promptTokens + completionTokens;

        LlmUsage usage = llmUsageRepository.findByUserIdAndUsageDateAndProviderAndModelAndByok(
                userId, today, provider, model, isByok
        ).orElseGet(() -> {
            LlmUsage u = new LlmUsage();
            u.setUser(userRepository.getReferenceById(userId));
            u.setUsageDate(today);
            u.setProvider(provider);
            u.setModel(model);
            u.setByok(isByok);
            return u;
        });

        usage.setCallCount(usage.getCallCount() + 1);
        usage.setPromptTokens(usage.getPromptTokens() + promptTokens);
        usage.setCompletionTokens(usage.getCompletionTokens() + completionTokens);
        usage.setTotalTokens(usage.getTotalTokens() + totalTokens);

        llmUsageRepository.save(usage);
        log.info("Recorded LLM usage: userId={}, tokens={}, byok={}", userId, totalTokens, isByok);
    }

    /**
     * Saves or replaces user's own BYOK key.
     */
    @Transactional
    public void saveUserKey(Long userId, String provider, String apiKey) {
        if (apiKey == null || apiKey.trim().length() < 8) {
            throw new com.swayam.pathprep.exception.BadRequestException("INVALID_PARAMETER", "Invalid API key format");
        }

        String trimmed = apiKey.trim();
        String encrypted = encryptionService.encrypt(trimmed);
        String hint = "..." + trimmed.substring(Math.max(0, trimmed.length() - 4));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.swayam.pathprep.exception.NotFoundException("USER_NOT_FOUND", "User not found"));

        UserLlmKey key = userLlmKeyRepository.findByUserId(userId).orElseGet(() -> {
            UserLlmKey k = new UserLlmKey();
            k.setUser(user);
            return k;
        });

        key.setProvider(provider != null ? provider.toUpperCase() : "GROQ");
        key.setEncryptedKey(encrypted);
        key.setKeyHint(hint);

        userLlmKeyRepository.save(key);
        log.info("Saved user LLM key: userId={}, provider={}, hint={}", userId, key.getProvider(), hint);
    }

    @Transactional
    public void deleteUserKey(Long userId) {
        userLlmKeyRepository.deleteByUserId(userId);
        log.info("Deleted user LLM key: userId={}", userId);
    }
}
