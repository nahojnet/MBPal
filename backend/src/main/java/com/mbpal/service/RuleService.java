package com.mbpal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbpal.api.dto.request.CreateRuleRequest;
import com.mbpal.api.dto.response.RuleResponse;
import com.mbpal.api.dto.response.RuleVersionResponse;
import com.mbpal.api.exception.ConflictException;
import com.mbpal.api.exception.ResourceNotFoundException;
import com.mbpal.api.exception.ValidationException;
import com.mbpal.domain.entity.Rule;
import com.mbpal.domain.entity.RuleVersion;
import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;
import com.mbpal.domain.enums.RuleVersionStatus;
import com.mbpal.repository.RuleRepository;
import com.mbpal.repository.RuleVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RuleService {

    private final RuleRepository ruleRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final ObjectMapper objectMapper;

    public RuleService(RuleRepository ruleRepository, RuleVersionRepository ruleVersionRepository, ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.ruleVersionRepository = ruleVersionRepository;
        this.objectMapper = objectMapper;
    }

    public List<RuleResponse> listRules(RuleScope scope, RuleSeverity severity, String active) {
        List<Rule> rules;
        if (scope != null && active != null) {
            rules = ruleRepository.findByScopeAndActiveFlag(scope, active);
        } else if (severity != null && active != null) {
            rules = ruleRepository.findBySeverityAndActiveFlag(severity, active);
        } else if (active != null) {
            rules = ruleRepository.findByActiveFlag(active);
        } else {
            rules = ruleRepository.findAll();
        }
        return rules.stream().map(this::toResponse).toList();
    }

    public RuleResponse getRule(String ruleCode) {
        Rule rule = ruleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", "ruleCode", ruleCode));
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse createRule(CreateRuleRequest request) {
        ruleRepository.findByRuleCode(request.ruleCode())
                .ifPresent(r -> {
                    throw new ConflictException("Rule", "ruleCode", request.ruleCode());
                });

        Rule rule = Rule.builder()
                .ruleCode(request.ruleCode())
                .scope(request.scope())
                .severity(request.severity())
                .description(request.description())
                .build();

        rule = ruleRepository.save(rule);

        RuleVersion version = RuleVersion.builder()
                .rule(rule)
                .semanticVersion("1.0.0")
                .conditionJson(toJson(request.version().conditionJson()))
                .effectJson(toJson(request.version().effectJson()))
                .explanation(request.version().explanation())
                .status(RuleVersionStatus.DRAFT)
                .build();

        ruleVersionRepository.save(version);
        rule.getVersions().add(version);

        return toResponse(rule);
    }

    @Transactional
    public RuleVersionResponse publishVersion(String ruleCode, Long versionId) {
        ruleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", "ruleCode", ruleCode));

        RuleVersion version = ruleVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("RuleVersion", "ruleVersionId", versionId));

        if (!version.getRule().getRuleCode().equals(ruleCode)) {
            throw new ResourceNotFoundException("RuleVersion", "ruleCode/versionId", ruleCode + "/" + versionId);
        }

        version.setStatus(RuleVersionStatus.ACTIVE);
        version.setPublishedAt(Instant.now());

        return toVersionResponse(ruleVersionRepository.save(version));
    }

    public ValidationResult validateRule(Map<String, Object> conditionJson, Map<String, Object> effectJson) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (conditionJson == null || conditionJson.isEmpty()) {
            errors.add("conditionJson must not be empty");
        }
        if (effectJson == null || effectJson.isEmpty()) {
            errors.add("effectJson must not be empty");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private RuleResponse toResponse(Rule rule) {
        List<RuleVersionResponse> versions = rule.getVersions().stream()
                .map(this::toVersionResponse)
                .toList();

        return new RuleResponse(
                rule.getRuleCode(),
                rule.getDomain(),
                rule.getScope(),
                rule.getSeverity(),
                rule.getDescription(),
                "Y".equals(rule.getActiveFlag()),
                versions
        );
    }

    private RuleVersionResponse toVersionResponse(RuleVersion version) {
        return new RuleVersionResponse(
                version.getRuleVersionId(),
                version.getSemanticVersion(),
                version.getConditionJson(),
                version.getEffectJson(),
                version.getExplanation(),
                version.getStatus(),
                version.getPublishedAt()
        );
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid JSON", List.of(e.getMessage()));
        }
    }

    public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {}
}
