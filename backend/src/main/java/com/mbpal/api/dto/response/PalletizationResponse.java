package com.mbpal.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PalletizationResponse(
        String executionId,
        String status,
        String externalOrderId,
        String customerId,
        String rulesetCode,
        Instant startedAt,
        Instant endedAt,
        Long durationMs,
        Integer totalPallets,
        Integer totalBoxes,
        BigDecimal globalScore,
        String errorMessage,
        List<PalletResponse> pallets,
        List<ViolationResponse> violations
) {}
