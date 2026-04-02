package com.mbpal.engine.model;

import com.mbpal.domain.enums.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Grouping {

    private String ruleCode;
    private String attribute;
    private boolean mandatory;
    private RuleSeverity severity;
}
