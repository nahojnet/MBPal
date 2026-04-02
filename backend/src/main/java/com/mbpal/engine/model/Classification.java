package com.mbpal.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Predicate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Classification {

    private String ruleCode;
    private Predicate<BoxInstance> filter;
    private String attribute;
    private String value;
}
