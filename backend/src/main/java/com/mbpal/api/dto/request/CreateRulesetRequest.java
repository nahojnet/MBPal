package com.mbpal.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRulesetRequest(
        @NotBlank String rulesetCode,
        @NotBlank String label,
        String description,
        @NotNull List<Long> ruleVersionIds
) {}
