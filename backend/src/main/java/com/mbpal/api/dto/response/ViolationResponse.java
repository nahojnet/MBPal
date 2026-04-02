package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.RuleSeverity;

import java.math.BigDecimal;

public record ViolationResponse(
        String ruleCode,
        RuleSeverity severity,
        String description,
        BigDecimal impactScore
) {}
