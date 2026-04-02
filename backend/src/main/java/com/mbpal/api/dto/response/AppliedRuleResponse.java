package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.RuleSeverity;

public record AppliedRuleResponse(
        String ruleCode,
        String version,
        RuleSeverity severity,
        String explanation
) {}
