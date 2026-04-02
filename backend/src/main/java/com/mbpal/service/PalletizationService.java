package com.mbpal.service;

import com.mbpal.api.dto.request.CompareRequest;
import com.mbpal.api.dto.request.PalletizationRequest;
import com.mbpal.api.dto.response.*;
import com.mbpal.api.exception.ResourceNotFoundException;
import com.mbpal.api.exception.ValidationException;
import com.mbpal.domain.entity.*;
import com.mbpal.domain.enums.ExecutionStatus;
import com.mbpal.domain.enums.OrderStatus;
import com.mbpal.orchestrator.PalletizationOrchestrator;
import com.mbpal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PalletizationService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RulesetRepository rulesetRepository;
    private final PalletizationExecutionRepository executionRepository;
    private final PalletRepository palletRepository;
    private final PalletItemRepository palletItemRepository;
    private final DecisionTraceRepository traceRepository;
    private final ConstraintViolationRepository violationRepository;
    private final PalletizationOrchestrator orchestrator;

    @Transactional
    public ExecutionSummaryResponse submit(PalletizationRequest request) {
        // Check for duplicate order
        if (orderRepository.findByExternalOrderId(request.externalOrderId()).isPresent()) {
            // Allow re-palletization of existing order
        }

        // Load or create order
        CustomerOrder order = orderRepository.findByExternalOrderId(request.externalOrderId())
                .orElseGet(() -> {
                    CustomerOrder newOrder = CustomerOrder.builder()
                            .externalOrderId(request.externalOrderId())
                            .customerId(request.customerId())
                            .customerName(request.customerName())
                            .warehouseCode(request.warehouseCode())
                            .status(OrderStatus.RECEIVED)
                            .build();
                    return orderRepository.save(newOrder);
                });

        // Create order lines if new order
        if (order.getLines().isEmpty()) {
            int lineNum = 1;
            for (var lineReq : request.lines()) {
                Product product = productRepository.findByProductCode(lineReq.productCode())
                        .orElseThrow(() -> new ValidationException(
                                List.of("Produit inconnu: " + lineReq.productCode())));
                OrderLine line = OrderLine.builder()
                        .order(order)
                        .product(product)
                        .boxQuantity(lineReq.boxQuantity())
                        .lineNumber(lineNum++)
                        .build();
                order.getLines().add(line);
            }
            orderRepository.save(order);
        }

        // Load ruleset
        Ruleset ruleset = rulesetRepository.findByRulesetCode(request.rulesetCode())
                .orElseThrow(() -> new ValidationException(
                        List.of("Ruleset inconnu: " + request.rulesetCode())));

        // Create execution
        String executionCode = "PAL-EXEC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PalletizationExecution execution = PalletizationExecution.builder()
                .executionCode(executionCode)
                .order(order)
                .ruleset(ruleset)
                .status(ExecutionStatus.PENDING)
                .dryRunFlag("N")
                .build();
        execution = executionRepository.save(execution);

        // Launch async
        List<String> allowedSupports = request.supportPolicy() != null
                ? request.supportPolicy().allowedSupports()
                : null;
        orchestrator.executePalletization(execution.getExecutionId(), allowedSupports);

        return new ExecutionSummaryResponse(
                executionCode,
                order.getExternalOrderId(),
                order.getCustomerId(),
                "PENDING",
                null, null, null, null
        );
    }

    public PalletizationResponse getResult(String executionId) {
        PalletizationExecution execution = executionRepository.findByExecutionCode(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution introuvable: " + executionId));

        List<PalletResponse> palletResponses = new ArrayList<>();
        if (execution.getStatus() == ExecutionStatus.COMPLETED) {
            List<Pallet> pallets = palletRepository
                    .findByExecution_ExecutionIdOrderByPalletNumberAsc(execution.getExecutionId());
            for (Pallet p : pallets) {
                List<PalletItem> items = palletItemRepository
                        .findByPallet_PalletIdOrderByLayerNoAscPositionNoAsc(p.getPalletId());
                List<PalletItemResponse> itemResponses = items.stream()
                        .map(i -> new PalletItemResponse(
                                i.getProduct().getProductCode(),
                                i.getBoxInstanceIndex(),
                                i.getLayerNo(),
                                i.getPositionNo(),
                                i.getStackingClass()
                        ))
                        .collect(Collectors.toList());

                palletResponses.add(new PalletResponse(
                        p.getPalletNumber(),
                        p.getSupportType().getSupportCode(),
                        p.getTotalWeightKg(),
                        p.getTotalHeightMm(),
                        p.getFillRatePct(),
                        p.getStabilityScore(),
                        p.getLayerCount(),
                        p.getBoxCount(),
                        itemResponses
                ));
            }
        }

        List<ViolationResponse> violations = violationRepository
                .findByExecution_ExecutionId(execution.getExecutionId())
                .stream()
                .map(v -> new ViolationResponse(
                        v.getRuleVersion() != null ? v.getRuleVersion().getRule().getRuleCode() : null,
                        v.getSeverity().name(),
                        v.getDescription(),
                        v.getImpactScore()
                ))
                .collect(Collectors.toList());

        return new PalletizationResponse(
                execution.getExecutionCode(),
                execution.getStatus().name(),
                execution.getOrder().getExternalOrderId(),
                execution.getOrder().getCustomerId(),
                execution.getRuleset().getRulesetCode(),
                execution.getStartedAt(),
                execution.getEndedAt(),
                execution.getDurationMs(),
                execution.getTotalPallets(),
                execution.getTotalBoxes(),
                execution.getGlobalScore(),
                execution.getErrorMessage(),
                palletResponses,
                violations
        );
    }

    public ExplanationResponse getExplanations(String executionId) {
        PalletizationExecution execution = executionRepository.findByExecutionCode(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution introuvable: " + executionId));

        List<TraceResponse> traces = traceRepository
                .findByExecution_ExecutionIdOrderByTraceOrderAsc(execution.getExecutionId())
                .stream()
                .map(t -> new TraceResponse(t.getTraceOrder(), t.getStepName(), t.getDescription()))
                .collect(Collectors.toList());

        List<ViolationResponse> violations = violationRepository
                .findByExecution_ExecutionId(execution.getExecutionId())
                .stream()
                .map(v -> new ViolationResponse(
                        v.getRuleVersion() != null ? v.getRuleVersion().getRule().getRuleCode() : null,
                        v.getSeverity().name(),
                        v.getDescription(),
                        v.getImpactScore()
                ))
                .collect(Collectors.toList());

        return new ExplanationResponse(
                execution.getExecutionCode(),
                execution.getRuleset().getRulesetCode(),
                List.of(), // appliedRules populated from traces
                violations,
                traces
        );
    }

    public Page<ExecutionSummaryResponse> listExecutions(String orderId, String customerId,
                                                          String status, Pageable pageable) {
        // Simple implementation: if orderId is provided, filter by it
        if (orderId != null && !orderId.isEmpty()) {
            List<PalletizationExecution> execs = executionRepository.findByOrder_ExternalOrderId(orderId);
            List<ExecutionSummaryResponse> summaries = execs.stream()
                    .map(this::toSummary)
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(summaries, pageable, summaries.size());
        }

        Page<PalletizationExecution> page = executionRepository.findAll(pageable);
        return page.map(this::toSummary);
    }

    public CompareResponse compare(CompareRequest request) {
        PalletizationExecution exec1 = executionRepository.findByExecutionCode(request.executionId1())
                .orElseThrow(() -> new ResourceNotFoundException("Execution introuvable: " + request.executionId1()));
        PalletizationExecution exec2 = executionRepository.findByExecutionCode(request.executionId2())
                .orElseThrow(() -> new ResourceNotFoundException("Execution introuvable: " + request.executionId2()));

        ExecutionSummaryResponse sum1 = toSummary(exec1);
        ExecutionSummaryResponse sum2 = toSummary(exec2);

        int palletDiff = (exec2.getTotalPallets() != null ? exec2.getTotalPallets() : 0)
                - (exec1.getTotalPallets() != null ? exec1.getTotalPallets() : 0);
        BigDecimal scoreDiff = (exec2.getGlobalScore() != null ? exec2.getGlobalScore() : BigDecimal.ZERO)
                .subtract(exec1.getGlobalScore() != null ? exec1.getGlobalScore() : BigDecimal.ZERO);

        return new CompareResponse(sum1, sum2,
                new CompareResponse.Differences(palletDiff, scoreDiff));
    }

    private ExecutionSummaryResponse toSummary(PalletizationExecution e) {
        return new ExecutionSummaryResponse(
                e.getExecutionCode(),
                e.getOrder().getExternalOrderId(),
                e.getOrder().getCustomerId(),
                e.getStatus().name(),
                e.getTotalPallets(),
                e.getGlobalScore(),
                e.getStartedAt(),
                e.getDurationMs()
        );
    }
}
