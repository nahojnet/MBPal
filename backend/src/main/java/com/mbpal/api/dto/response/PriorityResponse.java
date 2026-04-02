package com.mbpal.api.dto.response;

import java.math.BigDecimal;

public record PriorityResponse(
        String ruleCode,
        Integer priorityOrder,
        BigDecimal weight
) {}
