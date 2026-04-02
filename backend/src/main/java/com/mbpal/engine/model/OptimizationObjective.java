package com.mbpal.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OptimizationObjective {

    private String ruleCode;
    private ObjectiveType type;
    private BigDecimal weight;
    private String attribute;
}
