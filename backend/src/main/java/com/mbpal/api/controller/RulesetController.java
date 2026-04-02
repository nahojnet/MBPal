package com.mbpal.api.controller;

import com.mbpal.api.dto.request.CreateRulesetRequest;
import com.mbpal.api.dto.request.UpdatePrioritiesRequest;
import com.mbpal.api.dto.response.PriorityResponse;
import com.mbpal.api.dto.response.RulesetResponse;
import com.mbpal.domain.enums.RuleVersionStatus;
import com.mbpal.service.RulesetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rulesets")
public class RulesetController {

    private final RulesetService rulesetService;

    public RulesetController(RulesetService rulesetService) {
        this.rulesetService = rulesetService;
    }

    @GetMapping
    public ResponseEntity<List<RulesetResponse>> listRulesets(
            @RequestParam(required = false) RuleVersionStatus status) {
        return ResponseEntity.ok(rulesetService.listRulesets(status));
    }

    @GetMapping("/{rulesetCode}")
    public ResponseEntity<RulesetResponse> getRuleset(@PathVariable String rulesetCode) {
        return ResponseEntity.ok(rulesetService.getRuleset(rulesetCode));
    }

    @PostMapping
    public ResponseEntity<RulesetResponse> createRuleset(@Valid @RequestBody CreateRulesetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rulesetService.createRuleset(request));
    }

    @PostMapping("/{rulesetCode}/publish")
    public ResponseEntity<RulesetResponse> publishRuleset(@PathVariable String rulesetCode) {
        return ResponseEntity.ok(rulesetService.publishRuleset(rulesetCode));
    }

    @GetMapping("/{rulesetCode}/priorities")
    public ResponseEntity<List<PriorityResponse>> getPriorities(@PathVariable String rulesetCode) {
        return ResponseEntity.ok(rulesetService.getPriorities(rulesetCode));
    }

    @PutMapping("/{rulesetCode}/priorities")
    public ResponseEntity<List<PriorityResponse>> updatePriorities(
            @PathVariable String rulesetCode,
            @Valid @RequestBody UpdatePrioritiesRequest request) {
        return ResponseEntity.ok(rulesetService.updatePriorities(rulesetCode, request));
    }
}
