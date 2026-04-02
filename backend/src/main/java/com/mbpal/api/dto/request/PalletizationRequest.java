package com.mbpal.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PalletizationRequest(
        @NotBlank String externalOrderId,
        @NotBlank String customerId,
        String customerName,
        String warehouseCode,
        @Valid SupportPolicy supportPolicy,
        @NotBlank String rulesetCode,
        @NotNull List<@Valid OrderLineRequest> lines
) {
    public record SupportPolicy(
            List<String> allowedSupports
    ) {}

    public record OrderLineRequest(
            @NotBlank String productCode,
            @NotNull @Positive Integer boxQuantity
    ) {}
}
