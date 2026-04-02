package com.mbpal.solver.algorithm;

import com.mbpal.domain.entity.SupportType;
import com.mbpal.domain.enums.RuleSeverity;
import com.mbpal.engine.model.*;
import com.mbpal.solver.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PalletizationSolver {

    private static final long MAX_IMPROVEMENT_MS = 2000;

    private int traceOrder;

    public PalletizationPlan solve(List<BoxInstance> boxes, ConstraintSet constraints,
                                   List<SupportType> availableSupports) {
        long startTime = System.currentTimeMillis();
        traceOrder = 0;
        List<TraceRecord> traces = new ArrayList<>();
        List<ViolationRecord> violations = new ArrayList<>();
        List<SolverPallet> allPallets = new ArrayList<>();

        // Edge case: empty box list
        if (boxes == null || boxes.isEmpty()) {
            log.info("No boxes to palletize");
            traces.add(trace("INIT", "No boxes provided, returning empty plan", null));
            return PalletizationPlan.builder()
                    .pallets(Collections.emptyList())
                    .violations(Collections.emptyList())
                    .globalScore(BigDecimal.ZERO)
                    .traces(traces)
                    .computeTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        log.info("Starting palletization for {} boxes with {} available supports",
                boxes.size(), availableSupports.size());

        // ── Step 1: Group boxes by mandatory groupings ──────────────────────────
        Map<String, List<BoxInstance>> groups = groupBoxesByMandatoryGroupings(boxes, constraints, traces);

        // ── Step 2 & 3: For each group, determine supports and bin-pack ─────────
        int palletCounter = 1;
        for (Map.Entry<String, List<BoxInstance>> entry : groups.entrySet()) {
            String groupKey = entry.getKey();
            List<BoxInstance> groupBoxes = entry.getValue();

            // Step 2: Determine allowed supports for this group
            SupportSelection supportSelection = determineSupports(groupKey, constraints,
                    availableSupports, violations, traces);

            // Step 3: Bin packing (Best Fit Decreasing)
            List<SolverPallet> groupPallets = binPackGroup(groupBoxes, groupKey, constraints,
                    supportSelection, traces, palletCounter);
            palletCounter += groupPallets.size();
            allPallets.addAll(groupPallets);
        }

        // ── Step 4: Layer assignment ────────────────────────────────────────────
        List<SolverPallet> afterLayerAssignment = new ArrayList<>();
        for (SolverPallet pallet : allPallets) {
            List<SolverPallet> result = assignLayers(pallet, constraints, traces, palletCounter);
            palletCounter += Math.max(0, result.size() - 1); // overflow pallets get new numbers
            afterLayerAssignment.addAll(result);
        }
        allPallets = afterLayerAssignment;

        // Renumber pallets sequentially
        for (int i = 0; i < allPallets.size(); i++) {
            allPallets.get(i).setPalletNumber(i + 1);
        }

        // ── Step 5: Local improvement ───────────────────────────────────────────
        allPallets = localImprovement(allPallets, constraints, availableSupports, traces);

        // Renumber pallets again after merges
        for (int i = 0; i < allPallets.size(); i++) {
            allPallets.get(i).setPalletNumber(i + 1);
        }

        // ── Step 6: Validation and scoring ──────────────────────────────────────
        validateHardConstraints(allPallets, constraints, violations, traces);
        BigDecimal globalScore = calculateGlobalScore(allPallets, constraints, traces);

        long computeTimeMs = System.currentTimeMillis() - startTime;
        traces.add(trace("COMPLETE", String.format("Solver completed in %d ms, %d pallets produced",
                computeTimeMs, allPallets.size()), null));
        log.info("Palletization completed in {} ms: {} pallets, {} violations",
                computeTimeMs, allPallets.size(), violations.size());

        return PalletizationPlan.builder()
                .pallets(allPallets)
                .violations(violations)
                .globalScore(globalScore)
                .traces(traces)
                .computeTimeMs(computeTimeMs)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Step 1: Group boxes by mandatory groupings
    // ═══════════════════════════════════════════════════════════════════════════

    private Map<String, List<BoxInstance>> groupBoxesByMandatoryGroupings(
            List<BoxInstance> boxes, ConstraintSet constraints, List<TraceRecord> traces) {

        List<Grouping> mandatoryGroupings = constraints.getGroupings().stream()
                .filter(g -> g.isMandatory() || g.getSeverity() == RuleSeverity.HARD)
                .collect(Collectors.toList());

        if (mandatoryGroupings.isEmpty()) {
            traces.add(trace("GROUP", String.format(
                    "No mandatory groupings, 1 group created: ALL (%d boxes)", boxes.size()), null));
            Map<String, List<BoxInstance>> singleGroup = new LinkedHashMap<>();
            singleGroup.put("ALL", new ArrayList<>(boxes));
            return singleGroup;
        }

        // Build composite group key from all mandatory grouping attributes
        Map<String, List<BoxInstance>> groups = new LinkedHashMap<>();
        for (BoxInstance box : boxes) {
            String key = buildGroupKey(box, mandatoryGroupings);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(box);
        }

        StringBuilder desc = new StringBuilder();
        desc.append(groups.size()).append(" groups created: ");
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<BoxInstance>> e : groups.entrySet()) {
            parts.add(e.getKey() + " (" + e.getValue().size() + " boxes)");
        }
        desc.append(String.join(", ", parts));
        traces.add(trace("GROUP", desc.toString(), null));
        log.debug("Grouping result: {}", desc);

        return groups;
    }

    private String buildGroupKey(BoxInstance box, List<Grouping> groupings) {
        List<String> keyParts = new ArrayList<>();
        for (Grouping g : groupings) {
            String value = resolveGroupingAttribute(box, g.getAttribute());
            keyParts.add(value != null ? value : "UNKNOWN");
        }
        return String.join("|", keyParts);
    }

    private String resolveGroupingAttribute(BoxInstance box, String attribute) {
        if ("temperatureType".equals(attribute)) {
            return box.getTemperatureType() != null ? box.getTemperatureType().name() : null;
        }
        if ("temperatureGroup".equals(attribute)) {
            return box.getTemperatureGroup();
        }
        if ("fragilityLevel".equals(attribute)) {
            return box.getFragilityLevel() != null ? box.getFragilityLevel().name() : null;
        }
        if ("productCode".equals(attribute)) {
            return box.getProductCode();
        }
        if ("stackingClass".equals(attribute)) {
            return box.getStackingClass();
        }
        // Check custom attributes
        Object val = box.getAttributes().get(attribute);
        return val != null ? val.toString() : null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Step 2: Determine allowed supports for a group
    // ═══════════════════════════════════════════════════════════════════════════

    private SupportSelection determineSupports(String groupKey, ConstraintSet constraints,
                                               List<SupportType> availableSupports,
                                               List<ViolationRecord> violations,
                                               List<TraceRecord> traces) {

        List<SupportType> allowed = new ArrayList<>(availableSupports);
        SupportType preferred = null;
        SupportType required = null;

        for (SupportRule rule : constraints.getSupportRules()) {
            switch (rule.getType()) {
                case ALLOWED:
                    Set<String> allowedCodes = new HashSet<>(rule.getSupportCodes());
                    allowed = allowed.stream()
                            .filter(s -> allowedCodes.contains(s.getSupportCode()))
                            .collect(Collectors.toList());
                    break;
                case REQUIRED:
                    for (SupportType st : availableSupports) {
                        if (rule.getSupportCodes().contains(st.getSupportCode())) {
                            required = st;
                            break;
                        }
                    }
                    break;
                case PREFERRED:
                    for (SupportType st : allowed) {
                        if (rule.getSupportCodes().contains(st.getSupportCode())) {
                            preferred = st;
                            break;
                        }
                    }
                    break;
            }
        }

        if (required != null) {
            traces.add(trace("SUPPORT", String.format("Group %s: required support %s",
                    groupKey, required.getSupportCode()), null));
            return new SupportSelection(List.of(required), required);
        }

        if (allowed.isEmpty()) {
            violations.add(ViolationRecord.builder()
                    .ruleCode("SUPPORT_AVAILABILITY")
                    .severity(RuleSeverity.HARD)
                    .description("No valid support available for group " + groupKey)
                    .impactScore(BigDecimal.TEN)
                    .build());
            // Fall back to first available support
            if (!availableSupports.isEmpty()) {
                SupportType fallback = availableSupports.get(0);
                traces.add(trace("SUPPORT", String.format(
                        "Group %s: no allowed support, falling back to %s",
                        groupKey, fallback.getSupportCode()), null));
                return new SupportSelection(List.of(fallback), fallback);
            }
            // No supports at all - should not happen in practice
            traces.add(trace("SUPPORT", String.format(
                    "Group %s: no supports available at all", groupKey), null));
            return new SupportSelection(Collections.emptyList(), null);
        }

        SupportType effectivePreferred = preferred != null ? preferred : allowed.get(0);
        traces.add(trace("SUPPORT", String.format("Group %s: %d allowed supports, preferred=%s",
                groupKey, allowed.size(), effectivePreferred.getSupportCode()), null));
        return new SupportSelection(allowed, effectivePreferred);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Step 3: Bin packing (Best Fit Decreasing) per group
    // ═══════════════════════════════════════════════════════════════════════════

    private List<SolverPallet> binPackGroup(List<BoxInstance> groupBoxes, String groupKey,
                                            ConstraintSet constraints,
                                            SupportSelection supportSelection,
                                            List<TraceRecord> traces, int palletStartNumber) {

        List<SolverPallet> pallets = new ArrayList<>();

        if (supportSelection.preferred == null) {
            log.warn("No support type available for group {}; boxes cannot be placed", groupKey);
            traces.add(trace("BIN_PACK", String.format(
                    "Group %s: skipped (no support available)", groupKey), null));
            return pallets;
        }

        // Sort boxes by weight descending (heaviest first)
        List<BoxInstance> sorted = new ArrayList<>(groupBoxes);
        sorted.sort(Comparator.comparing(BoxInstance::getWeightKg).reversed());

        for (BoxInstance box : sorted) {
            // Edge case: single box too heavy for any support
            if (box.getWeightKg().compareTo(supportSelection.preferred.getMaxLoadKg()) > 0) {
                log.warn("Box {} weighs {} kg, exceeding max load {} kg of support {}",
                        box.getInstanceId(), box.getWeightKg(),
                        supportSelection.preferred.getMaxLoadKg(),
                        supportSelection.preferred.getSupportCode());
            }

            // Try to fit in existing pallets - pick best fit (least remaining capacity)
            SolverPallet bestPallet = null;
            BigDecimal bestRemaining = null;

            for (SolverPallet pallet : pallets) {
                if (pallet.canAddBox(box, constraints)) {
                    BigDecimal remaining = pallet.remainingWeight()
                            .subtract(box.getWeightKg());
                    if (bestPallet == null || remaining.compareTo(bestRemaining) < 0) {
                        bestPallet = pallet;
                        bestRemaining = remaining;
                    }
                }
            }

            if (bestPallet != null) {
                addBoxToPallet(bestPallet, box);
                traces.add(trace("BIN_PACK", String.format("Box %s (%.2f kg) -> pallet %d",
                        box.getInstanceId(), box.getWeightKg(), bestPallet.getPalletNumber()),
                        bestPallet.getPalletNumber()));
            } else {
                // Create new pallet
                int palletNum = palletStartNumber + pallets.size();
                SolverPallet newPallet = createPallet(palletNum, supportSelection.preferred, groupKey);
                addBoxToPallet(newPallet, box);
                pallets.add(newPallet);
                traces.add(trace("BIN_PACK", String.format(
                        "New pallet %d (support=%s) for box %s (%.2f kg)",
                        palletNum, supportSelection.preferred.getSupportCode(),
                        box.getInstanceId(), box.getWeightKg()), palletNum));
            }
        }

        traces.add(trace("BIN_PACK", String.format("Group %s: %d boxes packed into %d pallets",
                groupKey, groupBoxes.size(), pallets.size()), null));
        return pallets;
    }

    private SolverPallet createPallet(int palletNumber, SupportType support, String temperatureGroup) {
        return SolverPallet.builder()
                .palletNumber(palletNumber)
                .supportCode(support.getSupportCode())
                .maxLoadKg(support.getMaxLoadKg())
                .maxTotalHeightMm(support.getMaxTotalHeightMm())
                .supportHeightMm(support.getHeightMm())
                .currentWeightKg(BigDecimal.ZERO)
                .currentHeightMm(support.getHeightMm())
                .temperatureGroup("ALL".equals(temperatureGroup) ? null : temperatureGroup)
                .merged(false)
                .items(new ArrayList<>())
                .build();
    }

    private void addBoxToPallet(SolverPallet pallet, BoxInstance box) {
        pallet.getItems().add(SolverPalletItem.builder()
                .box(box)
                .stackingClass(box.getStackingClass())
                .build());
        pallet.setCurrentWeightKg(pallet.getCurrentWeightKg().add(box.getWeightKg()));
        pallet.setCurrentHeightMm(pallet.getCurrentHeightMm() + box.getHeightMm());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Step 4: Layer assignment (intra-pallet ordering)
    // ═══════════════════════════════════════════════════════════════════════════

    private List<SolverPallet> assignLayers(SolverPallet pallet, ConstraintSet constraints,
                                            List<TraceRecord> traces, int nextPalletNumber) {
        List<SolverPallet> result = new ArrayList<>();

        // Separate items by stacking class
        List<SolverPalletItem> bottomItems = new ArrayList<>();
        List<SolverPalletItem> middleItems = new ArrayList<>();
        List<SolverPalletItem> topItems = new ArrayList<>();

        for (SolverPalletItem item : pallet.getItems()) {
            String sc = item.getStackingClass();
            if ("BOTTOM".equalsIgnoreCase(sc)) {
                bottomItems.add(item);
            } else if ("TOP".equalsIgnoreCase(sc)) {
                topItems.add(item);
            } else {
                middleItems.add(item);
            }
        }

        // Within each class, sort by weight descending
        Comparator<SolverPalletItem> byWeightDesc =
                Comparator.comparing((SolverPalletItem i) -> i.getBox().getWeightKg()).reversed();
        bottomItems.sort(byWeightDesc);
        middleItems.sort(byWeightDesc);
        topItems.sort(byWeightDesc);

        // Assign layer numbers sequentially
        List<SolverPalletItem> ordered = new ArrayList<>();
        ordered.addAll(bottomItems);
        ordered.addAll(middleItems);
        ordered.addAll(topItems);

        int layerNo = 1;
        for (SolverPalletItem item : ordered) {
            item.setLayerNo(layerNo);
            item.setPositionNo(1);
            layerNo++;
        }

        // Recalculate total height
        int totalHeight = pallet.getSupportHeightMm();
        for (SolverPalletItem item : ordered) {
            totalHeight += item.getBox().getHeightMm();
        }

        // Check if height exceeds max - split overflow to new pallet if needed
        if (totalHeight > pallet.getMaxTotalHeightMm()) {
            List<SolverPalletItem> fits = new ArrayList<>();
            List<SolverPalletItem> overflow = new ArrayList<>();
            int runningHeight = pallet.getSupportHeightMm();

            for (SolverPalletItem item : ordered) {
                if (runningHeight + item.getBox().getHeightMm() <= pallet.getMaxTotalHeightMm()) {
                    fits.add(item);
                    runningHeight += item.getBox().getHeightMm();
                } else {
                    overflow.add(item);
                }
            }

            // Update original pallet with fitting items
            pallet.setItems(fits);
            recalculatePallet(pallet);
            result.add(pallet);

            traces.add(trace("LAYER", String.format(
                    "Pallet %d: height overflow, %d items kept, %d items moved to new pallet",
                    pallet.getPalletNumber(), fits.size(), overflow.size()),
                    pallet.getPalletNumber()));

            // Create overflow pallet(s) recursively
            if (!overflow.isEmpty()) {
                SolverPallet overflowPallet = SolverPallet.builder()
                        .palletNumber(nextPalletNumber)
                        .supportCode(pallet.getSupportCode())
                        .maxLoadKg(pallet.getMaxLoadKg())
                        .maxTotalHeightMm(pallet.getMaxTotalHeightMm())
                        .supportHeightMm(pallet.getSupportHeightMm())
                        .currentWeightKg(BigDecimal.ZERO)
                        .currentHeightMm(pallet.getSupportHeightMm())
                        .temperatureGroup(pallet.getTemperatureGroup())
                        .merged(false)
                        .items(new ArrayList<>(overflow))
                        .build();
                recalculatePallet(overflowPallet);

                // Recursively assign layers to overflow pallet
                List<SolverPallet> overflowResult = assignLayers(overflowPallet, constraints,
                        traces, nextPalletNumber + 1);
                result.addAll(overflowResult);
            }
        } else {
            pallet.setItems(ordered);
            pallet.setCurrentHeightMm(totalHeight);
            result.add(pallet);

            traces.add(trace("LAYER", String.format(
                    "Pallet %d: %d layers assigned, total height=%d mm",
                    pallet.getPalletNumber(), ordered.size(), totalHeight),
                    pallet.getPalletNumber()));
        }

        return result;
    }

    private void recalculatePallet(SolverPallet pallet) {
        BigDecimal weight = BigDecimal.ZERO;
        int height = pallet.getSupportHeightMm();
        int layerNo = 1;
        for (SolverPalletItem item : pallet.getItems()) {
            weight = weight.add(item.getBox().getWeightKg());
            height += item.getBox().getHeightMm();
            item.setLayerNo(layerNo);
            item.setPositionNo(1);
            layerNo++;
        }
        pallet.setCurrentWeightKg(weight);
        pallet.setCurrentHeightMm(height);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Step 5: Local improvement
    // ═══════════════════════════════════════════════════════════════════════════

    private List<SolverPallet> localImprovement(List<SolverPallet> pallets,
                                                ConstraintSet constraints,
                                                List<SupportType> availableSupports,
                                                List<TraceRecord> traces) {
        long improvementStart = System.currentTimeMillis();
        boolean improved = true;

        // Build a lookup for support types by code
        Map<String, SupportType> supportByCode = availableSupports.stream()
                .collect(Collectors.toMap(SupportType::getSupportCode, s -> s, (a, b) -> a));

        // Build a lookup for merge targets
        Map<String, SupportType> mergeTargets = new HashMap<>();
        for (SupportType st : availableSupports) {
            if (st.isMergeable() && st.getMergeTargetCode() != null) {
                mergeTargets.put(st.getSupportCode(), supportByCode.get(st.getMergeTargetCode()));
            }
        }

        while (improved && (System.currentTimeMillis() - improvementStart) < MAX_IMPROVEMENT_MS) {
            improved = false;

            // Try merging under-filled pallets of same temperature group
            for (int i = 0; i < pallets.size() && !improved; i++) {
                for (int j = i + 1; j < pallets.size() && !improved; j++) {
                    SolverPallet p1 = pallets.get(i);
                    SolverPallet p2 = pallets.get(j);

                    // Same temperature group (or both null)
                    if (!Objects.equals(p1.getTemperatureGroup(), p2.getTemperatureGroup())) {
                        continue;
                    }

                    // Same support type - try simple merge
                    if (p1.getSupportCode().equals(p2.getSupportCode())) {
                        BigDecimal combinedWeight = p1.getCurrentWeightKg()
                                .add(p2.getCurrentWeightKg());
                        int combinedHeight = p1.getSupportHeightMm()
                                + (p1.getCurrentHeightMm() - p1.getSupportHeightMm())
                                + (p2.getCurrentHeightMm() - p2.getSupportHeightMm());

                        if (combinedWeight.compareTo(p1.getMaxLoadKg()) <= 0
                                && combinedHeight <= p1.getMaxTotalHeightMm()) {
                            // Merge p2 into p1
                            p1.getItems().addAll(p2.getItems());
                            recalculatePallet(p1);
                            pallets.remove(j);
                            improved = true;
                            traces.add(trace("IMPROVE", String.format(
                                    "Merged pallet %d into pallet %d (same support %s)",
                                    p2.getPalletNumber(), p1.getPalletNumber(),
                                    p1.getSupportCode()), p1.getPalletNumber()));
                        }
                    }

                    // Try mergeable support merge (e.g., two half-pallets into one full pallet)
                    if (!improved) {
                        SupportType target1 = mergeTargets.get(p1.getSupportCode());
                        SupportType target2 = mergeTargets.get(p2.getSupportCode());

                        if (target1 != null && target2 != null
                                && target1.getSupportCode().equals(target2.getSupportCode())) {
                            BigDecimal combinedWeight = p1.getCurrentWeightKg()
                                    .add(p2.getCurrentWeightKg());
                            int combinedBoxHeight = (p1.getCurrentHeightMm() - p1.getSupportHeightMm())
                                    + (p2.getCurrentHeightMm() - p2.getSupportHeightMm());
                            int combinedHeight = target1.getHeightMm() + combinedBoxHeight;

                            if (combinedWeight.compareTo(target1.getMaxLoadKg()) <= 0
                                    && combinedHeight <= target1.getMaxTotalHeightMm()) {
                                // Merge into a new pallet with the target support
                                p1.getItems().addAll(p2.getItems());
                                p1.setSupportCode(target1.getSupportCode());
                                p1.setMaxLoadKg(target1.getMaxLoadKg());
                                p1.setMaxTotalHeightMm(target1.getMaxTotalHeightMm());
                                p1.setSupportHeightMm(target1.getHeightMm());
                                p1.setMerged(true);
                                recalculatePallet(p1);
                                pallets.remove(j);
                                improved = true;
                                traces.add(trace("IMPROVE", String.format(
                                        "Merged half-pallets %d and %d into pallet %d (target support %s)",
                                        p1.getPalletNumber(), p2.getPalletNumber(),
                                        p1.getPalletNumber(), target1.getSupportCode()),
                                        p1.getPalletNumber()));
                            }
                        }
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - improvementStart;
        traces.add(trace("IMPROVE", String.format("Local improvement completed in %d ms", elapsed), null));
        return pallets;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Step 6: Validation and scoring
    // ═══════════════════════════════════════════════════════════════════════════

    private void validateHardConstraints(List<SolverPallet> pallets, ConstraintSet constraints,
                                         List<ViolationRecord> violations,
                                         List<TraceRecord> traces) {
        for (SolverPallet pallet : pallets) {
            // Weight constraint
            if (pallet.getCurrentWeightKg().compareTo(pallet.getMaxLoadKg()) > 0) {
                violations.add(ViolationRecord.builder()
                        .ruleCode("WEIGHT_EXCEEDED")
                        .severity(RuleSeverity.HARD)
                        .description(String.format("Pallet %d: weight %.2f kg exceeds max %.2f kg",
                                pallet.getPalletNumber(), pallet.getCurrentWeightKg(),
                                pallet.getMaxLoadKg()))
                        .impactScore(BigDecimal.TEN)
                        .palletNumber(pallet.getPalletNumber())
                        .build());
            }

            // Height constraint
            if (pallet.getCurrentHeightMm() > pallet.getMaxTotalHeightMm()) {
                violations.add(ViolationRecord.builder()
                        .ruleCode("HEIGHT_EXCEEDED")
                        .severity(RuleSeverity.HARD)
                        .description(String.format("Pallet %d: height %d mm exceeds max %d mm",
                                pallet.getPalletNumber(), pallet.getCurrentHeightMm(),
                                pallet.getMaxTotalHeightMm()))
                        .impactScore(BigDecimal.TEN)
                        .palletNumber(pallet.getPalletNumber())
                        .build());
            }

            // Temperature grouping constraint
            if (pallet.getTemperatureGroup() != null) {
                for (SolverPalletItem item : pallet.getItems()) {
                    String boxTempGroup = item.getBox().getTemperatureType() != null
                            ? item.getBox().getTemperatureType().name() : null;
                    if (boxTempGroup != null && !pallet.getTemperatureGroup().equals(boxTempGroup)) {
                        boolean isMandatory = constraints.getGroupings().stream()
                                .anyMatch(g -> "temperatureType".equals(g.getAttribute())
                                        && g.isMandatory());
                        if (isMandatory) {
                            violations.add(ViolationRecord.builder()
                                    .ruleCode("TEMPERATURE_MIX")
                                    .severity(RuleSeverity.HARD)
                                    .description(String.format(
                                            "Pallet %d: box %s (%s) mixed with temperature group %s",
                                            pallet.getPalletNumber(), item.getBox().getInstanceId(),
                                            boxTempGroup, pallet.getTemperatureGroup()))
                                    .impactScore(BigDecimal.TEN)
                                    .palletNumber(pallet.getPalletNumber())
                                    .build());
                        }
                    }
                }
            }

            // Forbidden placement constraint
            for (ForbiddenPlacement fp : constraints.getForbiddenPlacements()) {
                if (fp.getSeverity() == RuleSeverity.HARD) {
                    List<SolverPalletItem> items = pallet.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        for (int j = i + 1; j < items.size(); j++) {
                            SolverPalletItem lower = items.get(i);
                            SolverPalletItem upper = items.get(j);
                            if (fp.getAboveFilter().test(upper.getBox())
                                    && fp.getBelowFilter().test(lower.getBox())) {
                                violations.add(ViolationRecord.builder()
                                        .ruleCode(fp.getRuleCode())
                                        .severity(RuleSeverity.HARD)
                                        .description(String.format(
                                                "Pallet %d: forbidden placement - box %s above box %s",
                                                pallet.getPalletNumber(),
                                                upper.getBox().getInstanceId(),
                                                lower.getBox().getInstanceId()))
                                        .impactScore(BigDecimal.valueOf(5))
                                        .palletNumber(pallet.getPalletNumber())
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        traces.add(trace("VALIDATE", String.format("Validation complete: %d violations found",
                violations.size()), null));
    }

    private BigDecimal calculateGlobalScore(List<SolverPallet> pallets,
                                            ConstraintSet constraints,
                                            List<TraceRecord> traces) {
        if (pallets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Fill rate: average across pallets
        BigDecimal totalFillRate = BigDecimal.ZERO;
        for (SolverPallet pallet : pallets) {
            int availableHeight = pallet.getMaxTotalHeightMm() - pallet.getSupportHeightMm();
            if (availableHeight > 0) {
                int usedHeight = pallet.getCurrentHeightMm() - pallet.getSupportHeightMm();
                BigDecimal fillRate = BigDecimal.valueOf(usedHeight)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(availableHeight), 2, RoundingMode.HALF_UP);
                totalFillRate = totalFillRate.add(fillRate);
            }
        }
        BigDecimal avgFillRate = totalFillRate.divide(
                BigDecimal.valueOf(pallets.size()), 2, RoundingMode.HALF_UP);

        // Stability score: % of pallets where heaviest box is in lowest layer
        int stableCount = 0;
        for (SolverPallet pallet : pallets) {
            if (pallet.getItems().isEmpty()) continue;
            BigDecimal maxWeight = pallet.getItems().stream()
                    .map(i -> i.getBox().getWeightKg())
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            // Check if heaviest is in layer 1
            boolean heaviestAtBottom = pallet.getItems().stream()
                    .anyMatch(i -> i.getLayerNo() == 1
                            && i.getBox().getWeightKg().compareTo(maxWeight) == 0);
            if (heaviestAtBottom) {
                stableCount++;
            }
        }
        BigDecimal stabilityScore = BigDecimal.valueOf(stableCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(pallets.size()), 2, RoundingMode.HALF_UP);

        // Weighted combination based on optimization objectives
        BigDecimal fillWeight = BigDecimal.valueOf(50);
        BigDecimal stabilityWeight = BigDecimal.valueOf(50);

        for (OptimizationObjective obj : constraints.getOptimizationObjectives()) {
            if (obj.getType() == ObjectiveType.MINIMIZE_VOID && obj.getWeight() != null) {
                fillWeight = obj.getWeight();
            }
            if (obj.getType() == ObjectiveType.MAXIMIZE_STABILITY && obj.getWeight() != null) {
                stabilityWeight = obj.getWeight();
            }
        }

        BigDecimal totalWeight = fillWeight.add(stabilityWeight);
        BigDecimal globalScore;
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            globalScore = avgFillRate.multiply(fillWeight)
                    .add(stabilityScore.multiply(stabilityWeight))
                    .divide(totalWeight, 2, RoundingMode.HALF_UP);
        } else {
            globalScore = avgFillRate.add(stabilityScore)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }

        traces.add(trace("SCORE", String.format(
                "Fill rate=%.1f%%, Stability=%.1f%%, Global score=%.2f",
                avgFillRate, stabilityScore, globalScore), null));
        log.info("Scoring: fillRate={}, stability={}, global={}", avgFillRate, stabilityScore, globalScore);

        return globalScore;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════════════

    private TraceRecord trace(String stepName, String description, Integer palletNumber) {
        return TraceRecord.builder()
                .traceOrder(++traceOrder)
                .stepName(stepName)
                .description(description)
                .palletNumber(palletNumber)
                .build();
    }

    /**
     * Internal holder for support selection results.
     */
    private static class SupportSelection {
        final List<SupportType> allowed;
        final SupportType preferred;

        SupportSelection(List<SupportType> allowed, SupportType preferred) {
            this.allowed = allowed;
            this.preferred = preferred;
        }
    }
}
