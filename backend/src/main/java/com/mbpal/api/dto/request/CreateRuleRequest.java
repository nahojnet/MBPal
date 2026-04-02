package com.mbpal.api.dto.request;

import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateRuleRequest(
        @NotBlank String ruleCode,
        @NotNull RuleScope scope,
        @NotNull RuleSeverity severity,
        @NotBlank String description,
        @NotNull @Valid RuleVersionRequest version
) {
    public record RuleVersionRequest(
            @NotNull Map<String, Object> conditionJson,
            @NotNull Map<String, Object> effectJson,
            @NotBlank String explanation
    ) {}
}
