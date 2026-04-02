package com.mbpal.api.controller;

import com.mbpal.api.dto.request.CreateRuleRequest;
import com.mbpal.api.dto.request.UpdateRuleVersionRequest;
import com.mbpal.api.dto.response.RuleResponse;
import com.mbpal.api.dto.response.RuleVersionResponse;
import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;
import com.mbpal.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ResponseEntity<List<RuleResponse>> listRules(
            @RequestParam(required = false) RuleScope scope,
            @RequestParam(required = false) RuleSeverity severity,
            @RequestParam(required = false) String active) {
        return ResponseEntity.ok(ruleService.listRules(scope, severity, active));
    }

    @GetMapping("/{ruleCode}")
    public ResponseEntity<RuleResponse> getRule(@PathVariable String ruleCode) {
        return ResponseEntity.ok(ruleService.getRule(ruleCode));
    }

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody CreateRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createRule(request));
    }

    @PostMapping("/{ruleCode}/versions/{versionId}/publish")
    public ResponseEntity<RuleVersionResponse> publishVersion(
            @PathVariable String ruleCode,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(ruleService.publishVersion(ruleCode, versionId));
    }

    @PostMapping("/validate")
    public ResponseEntity<RuleService.ValidationResult> validateRule(
            @Valid @RequestBody UpdateRuleVersionRequest request) {
        return ResponseEntity.ok(ruleService.validateRule(request.conditionJson(), request.effectJson()));
    }
}
