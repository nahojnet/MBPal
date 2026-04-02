package com.mbpal.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PalletResponse(
        Integer palletNumber,
        String supportType,
        BigDecimal totalWeightKg,
        Integer totalHeightMm,
        BigDecimal fillRatePct,
        BigDecimal stabilityScore,
        Integer layerCount,
        Integer boxCount,
        List<PalletItemResponse> items
) {}
