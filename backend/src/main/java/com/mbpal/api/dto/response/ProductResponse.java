package com.mbpal.api.dto.response;

import com.mbpal.domain.enums.FragilityLevel;
import com.mbpal.domain.enums.TemperatureType;

import java.math.BigDecimal;

public record ProductResponse(
        Long productId,
        String productCode,
        String label,
        TemperatureType temperatureType,
        Integer lengthMm,
        Integer widthMm,
        Integer heightMm,
        BigDecimal weightKg,
        FragilityLevel fragilityLevel,
        String stackableFlag,
        boolean active
) {}
