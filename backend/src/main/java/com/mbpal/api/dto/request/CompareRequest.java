package com.mbpal.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompareRequest(
        @NotBlank String executionId1,
        @NotBlank String executionId2
) {}
