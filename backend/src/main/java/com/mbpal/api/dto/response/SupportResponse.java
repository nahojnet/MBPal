package com.mbpal.api.dto.response;

import java.math.BigDecimal;

public record SupportResponse(
        Long supportTypeId,
        String supportCode,
        String label,
        Integer lengthMm,
        Integer widthMm,
        Integer heightMm,
        BigDecimal maxLoadKg,
        Integer maxTotalHeightMm,
        String mergeableFlag,
        String mergeTargetCode,
        boolean active
) {}
