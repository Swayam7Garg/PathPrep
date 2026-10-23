package com.swayam.pathprep.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateUserRequest(
        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        String name,

        @Size(min = 1, max = 50, message = "Branch must be between 1 and 50 characters")
        String branch,

        Integer gradYear,

        Long targetTrackId
) {}
