package com.mbpal.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConstraintSet {

    @Builder.Default
    private List<Classification> classifications = new ArrayList<>();

    @Builder.Default
    private List<Grouping> groupings = new ArrayList<>();

    @Builder.Default
    private List<ForbiddenPlacement> forbiddenPlacements = new ArrayList<>();

    @Builder.Default
    private List<CapacityLimit> capacityLimits = new ArrayList<>();

    @Builder.Default
    private List<SupportRule> supportRules = new ArrayList<>();

    @Builder.Default
    private List<OptimizationObjective> optimizationObjectives = new ArrayList<>();

    @Builder.Default
    private List<AppliedRule> appliedRules = new ArrayList<>();
}
