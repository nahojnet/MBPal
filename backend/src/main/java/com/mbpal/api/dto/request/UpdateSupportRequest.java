package com.mbpal.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateSupportRequest(
        @NotBlank String supportCode,
        @NotBlank String label,
        @NotNull @Positive Integer lengthMm,
        @NotNull @Positive Integer widthMm,
        @NotNull @Positive Integer heightMm,
        @NotNull @Positive BigDecimal maxLoadKg,
        @NotNull @Positive Integer maxTotalHeightMm,
        String mergeableFlag,
        String mergeTargetCode,
        Integer mergeQuantity
) {}
