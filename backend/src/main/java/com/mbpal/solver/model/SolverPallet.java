package com.mbpal.solver.model;

import com.mbpal.domain.enums.RuleSeverity;
import com.mbpal.engine.model.BoxInstance;
import com.mbpal.engine.model.ConstraintSet;
import com.mbpal.engine.model.Grouping;
import com.mbpal.engine.model.ForbiddenPlacement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolverPallet {

    private int palletNumber;
    private String supportCode;
    private BigDecimal maxLoadKg;
    private int maxTotalHeightMm;
    private int supportHeightMm;
    private BigDecimal currentWeightKg;
    private int currentHeightMm;
    private String temperatureGroup;
    private boolean merged;

    @Builder.Default
    private List<SolverPalletItem> items = new ArrayList<>();

    public BigDecimal remainingWeight() {
        return maxLoadKg.subtract(currentWeightKg);
    }

    public int remainingHeight() {
        return maxTotalHeightMm - currentHeightMm;
    }

    public boolean canAddBox(BoxInstance box, ConstraintSet constraints) {
        // Check weight
        if (currentWeightKg.add(box.getWeightKg()).compareTo(maxLoadKg) > 0) {
            return false;
        }
        // Check height
        if (currentHeightMm + box.getHeightMm() > maxTotalHeightMm) {
            return false;
        }
        // Check temperature grouping (if hard constraint)
        if (temperatureGroup != null && !temperatureGroup.equals(box.getTemperatureType().name())) {
            for (Grouping g : constraints.getGroupings()) {
                if ("temperatureType".equals(g.getAttribute()) && g.isMandatory()) {
                    return false;
                }
            }
        }
        // Check forbidden placements
        for (ForbiddenPlacement fp : constraints.getForbiddenPlacements()) {
            if (fp.getSeverity() == RuleSeverity.HARD) {
                for (SolverPalletItem existingItem : items) {
                    if (fp.getAboveFilter().test(box) && fp.getBelowFilter().test(existingItem.getBox())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
