package com.swayam.pathprep.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swayam.pathprep.enums.UserRole;

import java.time.Instant;

public record UserProfile(
        Long id,
        String email,
        String name,
        String pictureUrl,
        UserRole role,
        String branch,
        Integer gradYear,
        Long targetTrackId,
        String targetTrackName,
        InstitutionSummary institution,
        @JsonProperty("onboardingCompleted")
        boolean onboardingCompleted,
        Instant createdAt
) {}
