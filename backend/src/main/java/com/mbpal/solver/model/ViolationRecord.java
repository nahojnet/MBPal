package com.mbpal.solver.model;

import com.mbpal.domain.enums.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViolationRecord {

    private String ruleCode;
    private RuleSeverity severity;
    private String description;
    private BigDecimal impactScore;
    private Integer palletNumber;
}
