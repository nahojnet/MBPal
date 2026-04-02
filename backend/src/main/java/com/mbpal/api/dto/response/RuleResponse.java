package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;

import java.util.List;

public record RuleResponse(
        String ruleCode,
        String domain,
        RuleScope scope,
        RuleSeverity severity,
        String description,
        boolean active,
        List<RuleVersionResponse> versions
) {}
