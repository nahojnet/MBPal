package com.mbpal.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePrioritiesRequest(
        @NotNull List<@Valid PriorityEntry> priorities
) {
    public record PriorityEntry(
            @NotBlank String ruleCode,
            @NotNull @Positive Integer priorityOrder,
            @NotNull @Positive BigDecimal weight
    ) {}
}
