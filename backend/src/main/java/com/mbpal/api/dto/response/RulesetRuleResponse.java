package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.RuleSeverity;

public record RulesetRuleResponse(
        String ruleCode,
        String semanticVersion,
        RuleSeverity severity
) {}
