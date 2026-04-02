package com.mbpal.orchestrator;

import com.mbpal.domain.entity.*;
import com.mbpal.domain.enums.ExecutionStatus;
import com.mbpal.domain.enums.RuleVersionStatus;
import com.mbpal.engine.evaluator.RuleEngine;
import com.mbpal.engine.model.BoxInstance;
import com.mbpal.engine.model.ConstraintSet;
import com.mbpal.repository.*;
import com.mbpal.solver.algorithm.PalletizationSolver;
import com.mbpal.solver.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PalletizationOrchestrator {

    private final ProductRepository productRepository;
    private final SupportTypeRepository supportTypeRepository;
    private final RulesetRepository rulesetRepository;
    private final RulesetRuleRepository rulesetRuleRepository;
    private final RulePriorityRepository rulePriorityRepository;
    private final PalletizationExecutionRepository executionRepository;
    private final PalletRepository palletRepository;
    private final RuleEngine ruleEngine;
    private final PalletizationSolver solver;

    @Async
    @Transactional
    public void executePalletization(Long executionId, List<String> allowedSupportCodes) {
        PalletizationExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        execution.setStatus(ExecutionStatus.PROCESSING);
        execution.setStartedAt(Instant.now());
        executionRepository.save(execution);

        try {
            // Step 1: Load order lines and products
            CustomerOrder order = execution.getOrder();
            List<OrderLine> lines = order.getLines();

            // Step 2: Build box instances
            List<BoxInstance> boxes = new ArrayList<>();
            for (OrderLine line : lines) {
                Product product = line.getProduct();
                for (int i = 1; i <= line.getBoxQuantity(); i++) {
                    BoxInstance box = BoxInstance.builder()
                            .instanceId(product.getProductCode() + "-" + i)
                            .productCode(product.getProductCode())
                            .productId(product.getProductId())
                            .orderLineId(line.getOrderLineId())
                            .boxInstanceIndex(i)
                            .weightKg(product.getWeightKg())
                            .lengthMm(product.getLengthMm())
                            .widthMm(product.getWidthMm())
                            .heightMm(product.getHeightMm())
                            .temperatureType(product.getTemperatureType())
                            .fragilityLevel(product.getFragilityLevel())
                            .stackable(product.isStackable())
                            .maxStackWeightKg(product.getMaxStackWeightKg())
                            .attributes(new HashMap<>())
                            .build();
                    boxes.add(box);
                }
            }

            if (boxes.isEmpty()) {
                throw new RuntimeException("Aucun colis a palettiser");
            }

            // Step 3: Load ruleset and rules
            Ruleset ruleset = execution.getRuleset();
            List<RulesetRule> rulesetRules = rulesetRuleRepository.findByRuleset_RulesetId(ruleset.getRulesetId());
            List<RuleVersion> activeRuleVersions = rulesetRules.stream()
                    .map(RulesetRule::getRuleVersion)
                    .filter(rv -> rv.getStatus() == RuleVersionStatus.ACTIVE)
                    .collect(Collectors.toList());

            Map<Long, RulePriority> priorities = rulePriorityRepository
                    .findByRuleset_RulesetIdOrderByPriorityOrderAsc(ruleset.getRulesetId())
                    .stream()
                    .collect(Collectors.toMap(rp -> rp.getRule().getRuleId(), rp -> rp));

            // Step 4: Evaluate rules
            ConstraintSet constraintSet = ruleEngine.evaluate(activeRuleVersions, boxes, priorities);

            // Step 5: Load available supports
            List<SupportType> availableSupports;
            if (allowedSupportCodes != null && !allowedSupportCodes.isEmpty()) {
                availableSupports = supportTypeRepository.findBySupportCodeIn(allowedSupportCodes);
            } else {
                availableSupports = supportTypeRepository.findByActiveFlag("Y");
            }

            // Step 6: Solve
            PalletizationPlan plan = solver.solve(boxes, constraintSet, availableSupports);

            // Step 7: Persist results
            persistResults(execution, plan, lines);

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setEndedAt(Instant.now());
            execution.setDurationMs(plan.getComputeTimeMs());
            execution.setTotalPallets(plan.getPallets().size());
            execution.setTotalBoxes(boxes.size());
            execution.setGlobalScore(plan.getGlobalScore());
            executionRepository.save(execution);

            log.info("Palletization {} completed: {} pallets, score {}",
                    execution.getExecutionCode(), plan.getPallets().size(), plan.getGlobalScore());

        } catch (Exception e) {
            log.error("Palletization {} failed: {}", execution.getExecutionCode(), e.getMessage(), e);
            execution.setStatus(ExecutionStatus.ERROR);
            execution.setEndedAt(Instant.now());
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);
        }
    }

    private void persistResults(PalletizationExecution execution, PalletizationPlan plan, List<OrderLine> lines) {
        Map<Long, OrderLine> linesByProduct = new HashMap<>();
        for (OrderLine line : lines) {
            linesByProduct.put(line.getProduct().getProductId(), line);
        }

        for (SolverPallet sp : plan.getPallets()) {
            SupportType supportType = supportTypeRepository
                    .findBySupportCode(sp.getSupportCode())
                    .orElseThrow(() -> new RuntimeException("Support not found: " + sp.getSupportCode()));

            Pallet pallet = Pallet.builder()
                    .execution(execution)
                    .supportType(supportType)
                    .palletNumber(sp.getPalletNumber())
                    .totalWeightKg(sp.getCurrentWeightKg())
                    .totalHeightMm(sp.getCurrentHeightMm())
                    .fillRatePct(calculateFillRate(sp, supportType))
                    .stabilityScore(calculateStabilityScore(sp))
                    .layerCount(sp.getItems().stream().mapToInt(SolverPalletItem::getLayerNo).max().orElse(0))
                    .boxCount(sp.getItems().size())
                    .mergedFlag(sp.isMerged() ? "Y" : "N")
                    .build();

            for (SolverPalletItem item : sp.getItems()) {
                Product product = productRepository.findById(item.getBox().getProductId())
                        .orElseThrow();
                OrderLine orderLine = linesByProduct.get(item.getBox().getProductId());

                PalletItem pi = PalletItem.builder()
                        .pallet(pallet)
                        .product(product)
                        .orderLine(orderLine)
                        .boxInstanceIndex(item.getBox().getBoxInstanceIndex())
                        .layerNo(item.getLayerNo())
                        .positionNo(item.getPositionNo())
                        .stackingClass(item.getStackingClass())
                        .build();
                pallet.getItems().add(pi);
            }

            execution.getPallets().add(pallet);
        }

        // Persist traces
        int traceOrder = 1;
        for (TraceRecord trace : plan.getTraces()) {
            DecisionTrace dt = DecisionTrace.builder()
                    .execution(execution)
                    .traceOrder(traceOrder++)
                    .stepName(trace.getStepName())
                    .description(trace.getDescription())
                    .build();
            execution.getTraces().add(dt);
        }

        // Persist violations
        for (ViolationRecord vr : plan.getViolations()) {
            ConstraintViolation cv = ConstraintViolation.builder()
                    .execution(execution)
                    .severity(vr.getSeverity())
                    .description(vr.getDescription())
                    .impactScore(vr.getImpactScore())
                    .build();
            execution.getViolations().add(cv);
        }
    }

    private BigDecimal calculateFillRate(SolverPallet sp, SupportType support) {
        long totalBoxVolume = sp.getItems().stream()
                .mapToLong(item -> (long) item.getBox().getLengthMm() * item.getBox().getWidthMm() * item.getBox().getHeightMm())
                .sum();
        long availableVolume = (long) support.getUsableLength() * support.getUsableWidth()
                * (support.getMaxTotalHeightMm() - support.getHeightMm());
        if (availableVolume == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalBoxVolume * 100.0 / availableVolume)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStabilityScore(SolverPallet sp) {
        if (sp.getItems().isEmpty()) return BigDecimal.valueOf(100);
        List<SolverPalletItem> items = sp.getItems();
        int minLayer = items.stream().mapToInt(SolverPalletItem::getLayerNo).min().orElse(1);
        List<SolverPalletItem> bottomItems = items.stream()
                .filter(i -> i.getLayerNo() == minLayer)
                .toList();
        BigDecimal maxWeightInPallet = items.stream()
                .map(i -> i.getBox().getWeightKg())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        boolean heaviestAtBottom = bottomItems.stream()
                .anyMatch(i -> i.getBox().getWeightKg().compareTo(maxWeightInPallet) == 0);
        return heaviestAtBottom ? BigDecimal.valueOf(90) : BigDecimal.valueOf(60);
    }
}
