package com.mbpal.solver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TraceRecord {

    private int traceOrder;
    private String stepName;
    private String description;
    private Integer palletNumber;
}
