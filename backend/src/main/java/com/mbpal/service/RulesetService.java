package com.mbpal.service;

import com.mbpal.api.dto.request.CreateRulesetRequest;
import com.mbpal.api.dto.request.UpdatePrioritiesRequest;
import com.mbpal.api.dto.response.PriorityResponse;
import com.mbpal.api.dto.response.RulesetResponse;
import com.mbpal.api.dto.response.RulesetRuleResponse;
import com.mbpal.api.exception.ConflictException;
import com.mbpal.api.exception.ResourceNotFoundException;
import com.mbpal.domain.entity.Rule;
import com.mbpal.domain.entity.RulePriority;
import com.mbpal.domain.entity.RuleVersion;
import com.mbpal.domain.entity.Ruleset;
import com.mbpal.domain.entity.RulesetRule;
import com.mbpal.domain.enums.RuleVersionStatus;
import com.mbpal.repository.RulePriorityRepository;
import com.mbpal.repository.RuleRepository;
import com.mbpal.repository.RuleVersionRepository;
import com.mbpal.repository.RulesetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RulesetService {

    private final RulesetRepository rulesetRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final RuleRepository ruleRepository;
    private final RulePriorityRepository rulePriorityRepository;

    public RulesetService(RulesetRepository rulesetRepository,
                          RuleVersionRepository ruleVersionRepository,
                          RuleRepository ruleRepository,
                          RulePriorityRepository rulePriorityRepository) {
        this.rulesetRepository = rulesetRepository;
        this.ruleVersionRepository = ruleVersionRepository;
        this.ruleRepository = ruleRepository;
        this.rulePriorityRepository = rulePriorityRepository;
    }

    public List<RulesetResponse> listRulesets(RuleVersionStatus status) {
        List<Ruleset> rulesets;
        if (status != null) {
            rulesets = rulesetRepository.findByStatus(status);
        } else {
            rulesets = rulesetRepository.findAll();
        }
        return rulesets.stream().map(this::toResponse).toList();
    }

    public RulesetResponse getRuleset(String rulesetCode) {
        Ruleset ruleset = rulesetRepository.findByRulesetCode(rulesetCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset", "rulesetCode", rulesetCode));
        return toResponse(ruleset);
    }

    @Transactional
    public RulesetResponse createRuleset(CreateRulesetRequest request) {
        rulesetRepository.findByRulesetCode(request.rulesetCode())
                .ifPresent(r -> {
                    throw new ConflictException("Ruleset", "rulesetCode", request.rulesetCode());
                });

        Ruleset ruleset = Ruleset.builder()
                .rulesetCode(request.rulesetCode())
                .label(request.label())
                .description(request.description())
                .status(RuleVersionStatus.DRAFT)
                .build();

        ruleset = rulesetRepository.save(ruleset);

        for (Long ruleVersionId : request.ruleVersionIds()) {
            RuleVersion ruleVersion = ruleVersionRepository.findById(ruleVersionId)
                    .orElseThrow(() -> new ResourceNotFoundException("RuleVersion", "ruleVersionId", ruleVersionId));

            RulesetRule rulesetRule = RulesetRule.builder()
                    .ruleset(ruleset)
                    .ruleVersion(ruleVersion)
                    .build();
            ruleset.getRulesetRules().add(rulesetRule);
        }

        return toResponse(rulesetRepository.save(ruleset));
    }

    @Transactional
    public RulesetResponse publishRuleset(String rulesetCode) {
        Ruleset ruleset = rulesetRepository.findByRulesetCode(rulesetCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset", "rulesetCode", rulesetCode));

        ruleset.setStatus(RuleVersionStatus.ACTIVE);
        ruleset.setPublishedAt(Instant.now());

        return toResponse(rulesetRepository.save(ruleset));
    }

    public List<PriorityResponse> getPriorities(String rulesetCode) {
        Ruleset ruleset = rulesetRepository.findByRulesetCode(rulesetCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset", "rulesetCode", rulesetCode));

        List<RulePriority> priorities = rulePriorityRepository
                .findByRuleset_RulesetIdOrderByPriorityOrderAsc(ruleset.getRulesetId());

        return priorities.stream().map(this::toPriorityResponse).toList();
    }

    @Transactional
    public List<PriorityResponse> updatePriorities(String rulesetCode, UpdatePrioritiesRequest request) {
        Ruleset ruleset = rulesetRepository.findByRulesetCode(rulesetCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ruleset", "rulesetCode", rulesetCode));

        // Remove existing priorities
        ruleset.getPriorities().clear();
        rulesetRepository.save(ruleset);

        for (UpdatePrioritiesRequest.PriorityEntry entry : request.priorities()) {
            Rule rule = ruleRepository.findByRuleCode(entry.ruleCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Rule", "ruleCode", entry.ruleCode()));

            RulePriority priority = RulePriority.builder()
                    .ruleset(ruleset)
                    .rule(rule)
                    .priorityOrder(entry.priorityOrder())
                    .weight(entry.weight())
                    .build();
            ruleset.getPriorities().add(priority);
        }

        rulesetRepository.save(ruleset);

        List<RulePriority> saved = rulePriorityRepository
                .findByRuleset_RulesetIdOrderByPriorityOrderAsc(ruleset.getRulesetId());

        return saved.stream().map(this::toPriorityResponse).toList();
    }

    private RulesetResponse toResponse(Ruleset ruleset) {
        List<RulesetRuleResponse> rules = ruleset.getRulesetRules().stream()
                .map(rr -> new RulesetRuleResponse(
                        rr.getRuleVersion().getRule().getRuleCode(),
                        rr.getRuleVersion().getSemanticVersion(),
                        rr.getRuleVersion().getRule().getSeverity()
                ))
                .toList();

        return new RulesetResponse(
                ruleset.getRulesetCode(),
                ruleset.getLabel(),
                ruleset.getDescription(),
                ruleset.getStatus(),
                ruleset.getPublishedAt(),
                rules
        );
    }

    private PriorityResponse toPriorityResponse(RulePriority priority) {
        return new PriorityResponse(
                priority.getRule().getRuleCode(),
                priority.getPriorityOrder(),
                priority.getWeight()
        );
    }
}
