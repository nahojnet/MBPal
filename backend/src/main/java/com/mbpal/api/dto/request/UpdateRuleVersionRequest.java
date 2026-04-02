package com.mbpal.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateRuleVersionRequest(
        @NotNull Map<String, Object> conditionJson,
        @NotNull Map<String, Object> effectJson,
        String explanation
) {}
