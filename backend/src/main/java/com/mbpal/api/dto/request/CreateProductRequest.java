package com.mbpal.api.dto.request;

import com.mbpal.domain.enums.FragilityLevel;
import com.mbpal.domain.enums.TemperatureType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String productCode,
        @NotBlank String label,
        @NotNull TemperatureType temperatureType,
        @NotNull @Positive Integer lengthMm,
        @NotNull @Positive Integer widthMm,
        @NotNull @Positive Integer heightMm,
        @NotNull @Positive BigDecimal weightKg,
        @NotNull FragilityLevel fragilityLevel,
        @NotBlank String stackableFlag
) {}
