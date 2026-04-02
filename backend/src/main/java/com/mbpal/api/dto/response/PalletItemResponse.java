package com.mbpal.api.dto.response;

public record PalletItemResponse(
        String productCode,
        Integer boxInstanceIndex,
        Integer layerNo,
        Integer positionNo,
        String stackingClass
) {}
