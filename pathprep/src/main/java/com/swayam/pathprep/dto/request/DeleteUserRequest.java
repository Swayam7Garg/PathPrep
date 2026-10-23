package com.swayam.pathprep.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DeleteUserRequest(
        @NotNull(message = "Confirmation must be provided")
        @Pattern(regexp = "^DELETE$", message = "Must equal 'DELETE' to confirm account deletion")
        String confirm
) {}
