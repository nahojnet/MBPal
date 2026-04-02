package com.mbpal.engine.model;

import com.mbpal.domain.enums.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Predicate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForbiddenPlacement {

    private String ruleCode;
    private Predicate<BoxInstance> aboveFilter;
    private Predicate<BoxInstance> belowFilter;
    private RuleSeverity severity;
}
