package com.mbpal.solver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PalletizationPlan {

    private List<SolverPallet> pallets;
    private List<ViolationRecord> violations;
    private BigDecimal globalScore;
    private List<TraceRecord> traces;
    private long computeTimeMs;
}
