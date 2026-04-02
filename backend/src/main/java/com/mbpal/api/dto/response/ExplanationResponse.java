package com.mbpal.api.dto.response;

import java.util.List;

public record ExplanationResponse(
        String executionId,
        String rulesetCode,
        List<AppliedRuleResponse> appliedRules,
        List<ViolationResponse> violations,
        List<TraceResponse> decisionTrace
) {}
