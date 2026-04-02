package com.mbpal.engine.evaluator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbpal.domain.entity.Rule;
import com.mbpal.domain.entity.RulePriority;
import com.mbpal.domain.entity.RuleVersion;
import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;
import com.mbpal.engine.interpreter.ConditionEvaluator;
import com.mbpal.engine.interpreter.EffectInterpreter;
import com.mbpal.engine.model.AppliedRule;
import com.mbpal.engine.model.BoxInstance;
import com.mbpal.engine.model.ConstraintSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Main rule-engine orchestrator.
 * <p>
 * Loads active {@link RuleVersion}s, compiles their condition/effect JSON,
 * evaluates them against the provided {@link BoxInstance} list, and produces
 * a {@link ConstraintSet} that the solver consumes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngine {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final ConditionEvaluator conditionEvaluator;
    private final EffectInterpreter effectInterpreter;

    /**
     * Evaluate all active rules against the given boxes and return the resulting constraints.
     *
     * @param activeRules the rule versions to evaluate (already filtered to ACTIVE / valid date range)
     * @param boxes       all box instances for the current palletization session
     * @param priorities  optional priority map keyed by rule ID
     * @return a fully populated {@link ConstraintSet}
     */
    public ConstraintSet evaluate(List<RuleVersion> activeRules,
                                  List<BoxInstance> boxes,
                                  Map<Long, RulePriority> priorities) {

        ConstraintSet constraintSet = ConstraintSet.builder().build();

        // Sort rules by priority order when available
        List<RuleVersion> sorted = activeRules.stream()
                .sorted(Comparator.comparingInt(rv -> {
                    Rule rule = rv.getRule();
                    if (rule == null) return Integer.MAX_VALUE;
                    RulePriority rp = priorities.get(rule.getRuleId());
                    return rp != null ? rp.getPriorityOrder() : Integer.MAX_VALUE;
                }))
                .toList();

        for (RuleVersion rv : sorted) {
            try {
                processRule(rv, boxes, constraintSet);
            } catch (Exception e) {
                String ruleCode = rv.getRule() != null ? rv.getRule().getRuleCode() : "unknown";
                log.error("Failed to process rule '{}' version '{}': {}",
                        ruleCode, rv.getSemanticVersion(), e.getMessage(), e);
            }
        }

        return constraintSet;
    }

    // ------------------------------------------------------------------ per-rule processing

    private void processRule(RuleVersion rv, List<BoxInstance> boxes, ConstraintSet constraintSet) throws Exception {
        Rule rule = rv.getRule();
        String ruleCode = rule.getRuleCode();
        RuleScope scope = rule.getScope();
        RuleSeverity severity = rule.getSeverity();

        Map<String, Object> conditionJson = parseJson(rv.getConditionJson());
        Map<String, Object> effectJson = parseJson(rv.getEffectJson());

        log.debug("Processing rule '{}' v{} scope={} severity={}",
                ruleCode, rv.getSemanticVersion(), scope, severity);

        switch (scope) {
            case PACKAGE -> processPackageScope(rv, conditionJson, effectJson, boxes, constraintSet);
            case PALLET -> processPalletScope(rv, conditionJson, effectJson, boxes, constraintSet);
            case INTER_PACKAGE -> processInterPackageScope(rv, conditionJson, effectJson, boxes, constraintSet);
        }
    }

    /**
     * PACKAGE scope: evaluate condition per box, apply effect for matched boxes.
     */
    private void processPackageScope(RuleVersion rv,
                                     Map<String, Object> conditionJson,
                                     Map<String, Object> effectJson,
                                     List<BoxInstance> boxes,
                                     ConstraintSet constraintSet) {
        Rule rule = rv.getRule();
        Predicate<BoxInstance> filter = conditionEvaluator.compileCondition(conditionJson);

        long matchedCount = boxes.stream().filter(filter).count();
        log.debug("Rule '{}': {} / {} boxes matched", rule.getRuleCode(), matchedCount, boxes.size());

        if (matchedCount > 0) {
            effectInterpreter.interpret(effectJson, rule.getRuleCode(), rule.getSeverity(),
                    filter, null, null, constraintSet);
            applyClassificationSideEffects(filter, effectJson, boxes);
        }

        trackApplied(rv, (int) matchedCount, constraintSet);
    }

    /**
     * PALLET scope: condition is stored for solver-time evaluation; constraint is always added.
     */
    private void processPalletScope(RuleVersion rv,
                                    Map<String, Object> conditionJson,
                                    Map<String, Object> effectJson,
                                    List<BoxInstance> boxes,
                                    ConstraintSet constraintSet) {
        Rule rule = rv.getRule();
        // For pallet-level rules the condition may reference pallet attributes (e.g., support.code).
        // We compile what we can; the solver will evaluate pallet-level predicates at runtime.
        Predicate<BoxInstance> filter = conditionEvaluator.compileCondition(conditionJson);

        effectInterpreter.interpret(effectJson, rule.getRuleCode(), rule.getSeverity(),
                filter, null, null, constraintSet);

        trackApplied(rv, boxes.size(), constraintSet);
    }

    /**
     * INTER_PACKAGE scope: compile separate predicates for packageA / packageB,
     * then produce ForbiddenPlacement or other inter-box constraints.
     */
    private void processInterPackageScope(RuleVersion rv,
                                          Map<String, Object> conditionJson,
                                          Map<String, Object> effectJson,
                                          List<BoxInstance> boxes,
                                          ConstraintSet constraintSet) {
        Rule rule = rv.getRule();

        Map.Entry<Predicate<BoxInstance>, Predicate<BoxInstance>> pair =
                conditionEvaluator.compileInterPackageCondition(conditionJson);

        Predicate<BoxInstance> filterA = pair.getKey();
        Predicate<BoxInstance> filterB = pair.getValue();

        long matchedA = boxes.stream().filter(filterA).count();
        long matchedB = boxes.stream().filter(filterB).count();

        log.debug("Rule '{}': packageA matched={}, packageB matched={}", rule.getRuleCode(), matchedA, matchedB);

        if (matchedA > 0 && matchedB > 0) {
            effectInterpreter.interpret(effectJson, rule.getRuleCode(), rule.getSeverity(),
                    null, filterA, filterB, constraintSet);
        }

        trackApplied(rv, (int) (matchedA + matchedB), constraintSet);
    }

    // ------------------------------------------------------------------ side effects

    /**
     * When a SET_ATTRIBUTE effect is encountered at the PACKAGE scope, directly mutate matching
     * box instances so that downstream rules can reference the new attribute values.
     */
    private void applyClassificationSideEffects(Predicate<BoxInstance> filter,
                                                 Map<String, Object> effectJson,
                                                 List<BoxInstance> boxes) {
        String type = (String) effectJson.get("type");
        if (!"SET_ATTRIBUTE".equals(type) && !"MUST_BE_AT_BOTTOM".equals(type) && !"MUST_BE_AT_TOP".equals(type)) {
            return;
        }

        String attribute;
        String value;

        if ("MUST_BE_AT_BOTTOM".equals(type)) {
            attribute = "stackingClass";
            value = "BOTTOM";
        } else if ("MUST_BE_AT_TOP".equals(type)) {
            attribute = "stackingClass";
            value = "TOP";
        } else {
            attribute = (String) effectJson.get("attribute");
            value = String.valueOf(effectJson.get("value"));
        }

        for (BoxInstance box : boxes) {
            if (!filter.test(box)) continue;

            // Apply to well-known fields directly
            if ("stackingClass".equals(attribute)) {
                box.setStackingClass(value);
            } else if ("temperatureGroup".equals(attribute)) {
                box.setTemperatureGroup(value);
            }
            // Always store in dynamic attributes map
            box.getAttributes().put(attribute, value);
        }
    }

    // ------------------------------------------------------------------ tracking

    private void trackApplied(RuleVersion rv, int matchedCount, ConstraintSet constraintSet) {
        Rule rule = rv.getRule();
        constraintSet.getAppliedRules().add(
                AppliedRule.builder()
                        .ruleCode(rule.getRuleCode())
                        .version(rv.getSemanticVersion())
                        .severity(rule.getSeverity())
                        .explanation(rv.getExplanation())
                        .matchedBoxCount(matchedCount)
                        .build()
        );
    }

    // ------------------------------------------------------------------ JSON parsing

    private Map<String, Object> parseJson(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, MAP_TYPE);
    }
}
