package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.RuleVersionStatus;

import java.time.Instant;
import java.util.List;

public record RulesetResponse(
        String rulesetCode,
        String label,
        String description,
        RuleVersionStatus status,
        Instant publishedAt,
        List<RulesetRuleResponse> rules
) {}
