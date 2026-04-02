package com.mbpal.engine.model;

import com.mbpal.domain.enums.FragilityLevel;
import com.mbpal.domain.enums.TemperatureType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoxInstance {

    private String instanceId;
    private String productCode;
    private Long productId;
    private Long orderLineId;
    private int boxInstanceIndex;
    private BigDecimal weightKg;
    private int lengthMm;
    private int widthMm;
    private int heightMm;
    private TemperatureType temperatureType;
    private FragilityLevel fragilityLevel;
    private boolean stackable;
    private BigDecimal maxStackWeightKg;

    /** Assigned by rule engine */
    private String stackingClass;
    private String temperatureGroup;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
