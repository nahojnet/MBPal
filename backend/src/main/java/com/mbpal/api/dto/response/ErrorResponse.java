package com.mbpal.api.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        Instant timestamp,
        String path,
        List<String> details
) {}
