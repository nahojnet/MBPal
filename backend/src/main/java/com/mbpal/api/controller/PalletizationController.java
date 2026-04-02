package com.mbpal.api.controller;

import com.mbpal.api.dto.request.CompareRequest;
import com.mbpal.api.dto.request.PalletizationRequest;
import com.mbpal.api.dto.request.SimulateRequest;
import com.mbpal.api.dto.response.*;
import com.mbpal.service.PalletizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/palletizations")
@RequiredArgsConstructor
public class PalletizationController {

    private final PalletizationService palletizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionSummaryResponse submit(@Valid @RequestBody PalletizationRequest request) {
        return palletizationService.submit(request);
    }

    @GetMapping("/{executionId}")
    public PalletizationResponse getResult(@PathVariable String executionId) {
        return palletizationService.getResult(executionId);
    }

    @GetMapping("/{executionId}/explanations")
    public ExplanationResponse getExplanations(@PathVariable String executionId) {
        return palletizationService.getExplanations(executionId);
    }

    @GetMapping
    public Page<ExecutionSummaryResponse> listExecutions(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return palletizationService.listExecutions(orderId, customerId, status, pageable);
    }

    @PostMapping("/compare")
    public CompareResponse compare(@Valid @RequestBody CompareRequest request) {
        return palletizationService.compare(request);
    }

    @PostMapping("/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionSummaryResponse simulate(@Valid @RequestBody SimulateRequest request) {
        // Simulate reuses submit with dry_run flag
        PalletizationRequest palRequest = new PalletizationRequest(
                request.externalOrderId(),
                null, null, null,
                request.supportPolicy(),
                request.rulesetCode(),
                null
        );
        return palletizationService.submit(palRequest);
    }
}
