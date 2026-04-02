package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.RuleVersionStatus;

import java.time.Instant;

public record RuleVersionResponse(
        Long ruleVersionId,
        String semanticVersion,
        String conditionJson,
        String effectJson,
        String explanation,
        RuleVersionStatus status,
        Instant publishedAt
) {}
