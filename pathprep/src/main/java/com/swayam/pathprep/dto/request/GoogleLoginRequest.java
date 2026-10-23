package com.swayam.pathprep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank(message = "Google ID token credential must not be blank")
        @Size(max = 4096, message = "Credential must not exceed 4096 characters")
        String credential
) {}
