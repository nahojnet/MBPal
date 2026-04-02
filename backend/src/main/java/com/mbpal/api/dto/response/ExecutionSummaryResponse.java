package com.mbpal.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionSummaryResponse(
        String executionId,
        String externalOrderId,
        String customerId,
        String status,
        Integer totalPallets,
        BigDecimal globalScore,
        Instant startedAt,
        Long durationMs
) {}
