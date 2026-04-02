package com.mbpal.solver.model;

import com.mbpal.engine.model.BoxInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolverPalletItem {

    private BoxInstance box;
    private int layerNo;
    private int positionNo;
    private String stackingClass;
}
