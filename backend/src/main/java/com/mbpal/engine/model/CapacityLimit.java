package com.mbpal.engine.model;

import com.mbpal.domain.enums.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.function.Predicate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CapacityLimit {

    private String ruleCode;
    private Predicate<String> supportFilter;
    private String constraint;
    private BigDecimal value;
    private String unit;
    private RuleSeverity severity;
}
