package com.mbpal.engine.model;

import com.mbpal.domain.enums.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupportRule {

    private String ruleCode;
    private SupportRuleType type;
    private List<String> supportCodes;
    private RuleSeverity severity;
}
