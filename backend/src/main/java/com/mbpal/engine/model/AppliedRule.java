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
public class AppliedRule {

    private String ruleCode;
    private String version;
    private RuleSeverity severity;
    private String explanation;
    private int matchedBoxCount;
}
