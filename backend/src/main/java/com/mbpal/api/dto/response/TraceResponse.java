package com.mbpal.api.dto.response;

public record TraceResponse(
        Integer traceOrder,
        String stepName,
        String description
) {}
