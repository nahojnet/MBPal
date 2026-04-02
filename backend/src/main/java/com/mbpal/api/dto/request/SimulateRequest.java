package com.mbpal.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SimulateRequest(
        @NotBlank String externalOrderId,
        @NotBlank String rulesetCode,
        PalletizationRequest.SupportPolicy supportPolicy
) {}
