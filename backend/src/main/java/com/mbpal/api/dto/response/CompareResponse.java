package com.mbpal.api.dto.response;

import java.math.BigDecimal;

public record CompareResponse(
        ExecutionSummaryResponse execution1,
        ExecutionSummaryResponse execution2,
        Differences differences
) {
    public record Differences(
            int palletCountDiff,
            BigDecimal scoreDiff
    ) {}
}
