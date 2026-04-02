package com.mbpal.engine.interpreter;

import com.mbpal.domain.enums.RuleSeverity;
import com.mbpal.engine.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Interprets an effect JSON (parsed as Map) and populates the {@link ConstraintSet}.
 */
@Slf4j
@Component
public class EffectInterpreter {

    /**
     * Interpret a single effect and add the resulting constraint to the {@link ConstraintSet}.
     *
     * @param effectJson  the parsed effect JSON
     * @param ruleCode    code of the rule that produced this effect
     * @param severity    severity from the parent rule
     * @param filter      compiled condition filter for the triggering boxes (may be null for pallet-level)
     * @param filterA     compiled filter for packageA (INTER_PACKAGE only, may be null)
     * @param filterB     compiled filter for packageB (INTER_PACKAGE only, may be null)
     * @param constraintSet  the target constraint set to populate
     */
    public void interpret(Map<String, Object> effectJson,
                          String ruleCode,
                          RuleSeverity severity,
                          Predicate<BoxInstance> filter,
                          Predicate<BoxInstance> filterA,
                          Predicate<BoxInstance> filterB,
                          ConstraintSet constraintSet) {

        String type = (String) effectJson.get("type");
        if (type == null) {
            log.warn("Effect JSON has no 'type' field for rule '{}'; skipping", ruleCode);
            return;
        }

        switch (type) {
            case "SET_ATTRIBUTE" -> handleSetAttribute(effectJson, ruleCode, filter, constraintSet);
            case "GROUP_BY" -> handleGroupBy(effectJson, ruleCode, severity, constraintSet);
            case "MUST_BE_TOGETHER" -> handleGroupBy(
                    Map.of("attribute", "productCode"), ruleCode, severity, constraintSet);
            case "MUST_NOT_BE_TOGETHER" -> handleForbiddenPlacement(
                    ruleCode, severity, filter, filter, constraintSet);
            case "MUST_BE_AT_BOTTOM" -> handleSetAttribute(
                    Map.of("attribute", "stackingClass", "value", "BOTTOM"), ruleCode, filter, constraintSet);
            case "MUST_BE_AT_TOP" -> handleSetAttribute(
                    Map.of("attribute", "stackingClass", "value", "TOP"), ruleCode, filter, constraintSet);
            case "FORBID_ABOVE" -> handleForbidAbove(effectJson, ruleCode, severity, filterA, filterB, constraintSet);
            case "STACKING_PRIORITY" -> handleSetAttribute(
                    Map.of("attribute", "stackingPriority", "value", String.valueOf(effectJson.get("priority"))),
                    ruleCode, filter, constraintSet);
            case "SET_CONSTRAINT" -> handleSetConstraint(effectJson, ruleCode, severity, constraintSet);
            case "ALLOWED_SUPPORTS" -> handleSupportRule(effectJson, ruleCode, severity,
                    SupportRuleType.ALLOWED, constraintSet);
            case "REQUIRED_SUPPORT" -> handleSupportRule(effectJson, ruleCode, severity,
                    SupportRuleType.REQUIRED, constraintSet);
            case "PREFERRED_SUPPORT" -> handleSupportRule(effectJson, ruleCode, severity,
                    SupportRuleType.PREFERRED, constraintSet);
            case "MINIMIZE_PALLETS" -> addObjective(ruleCode, ObjectiveType.MINIMIZE_PALLETS,
                    effectJson, constraintSet);
            case "MINIMIZE_VOID" -> addObjective(ruleCode, ObjectiveType.MINIMIZE_VOID,
                    effectJson, constraintSet);
            case "MAXIMIZE_STABILITY" -> addObjective(ruleCode, ObjectiveType.MAXIMIZE_STABILITY,
                    effectJson, constraintSet);
            case "MINIMIZE_MIX" -> addObjective(ruleCode, ObjectiveType.MINIMIZE_MIX,
                    effectJson, constraintSet);
            default -> log.warn("Unrecognized effect type '{}' for rule '{}'; skipping", type, ruleCode);
        }
    }

    // ------------------------------------------------------------------ handlers

    private void handleSetAttribute(Map<String, Object> effectJson,
                                    String ruleCode,
                                    Predicate<BoxInstance> filter,
                                    ConstraintSet constraintSet) {
        String attribute = (String) effectJson.get("attribute");
        String value = String.valueOf(effectJson.get("value"));
        constraintSet.getClassifications().add(
                Classification.builder()
                        .ruleCode(ruleCode)
                        .filter(filter != null ? filter : box -> true)
                        .attribute(attribute)
                        .value(value)
                        .build()
        );
    }

    private void handleGroupBy(Map<String, Object> effectJson,
                                String ruleCode,
                                RuleSeverity severity,
                                ConstraintSet constraintSet) {
        String attribute = (String) effectJson.get("attribute");
        constraintSet.getGroupings().add(
                Grouping.builder()
                        .ruleCode(ruleCode)
                        .attribute(attribute)
                        .mandatory(severity == RuleSeverity.HARD)
                        .severity(severity)
                        .build()
        );
    }

    private void handleForbiddenPlacement(String ruleCode,
                                          RuleSeverity severity,
                                          Predicate<BoxInstance> aboveFilter,
                                          Predicate<BoxInstance> belowFilter,
                                          ConstraintSet constraintSet) {
        constraintSet.getForbiddenPlacements().add(
                ForbiddenPlacement.builder()
                        .ruleCode(ruleCode)
                        .aboveFilter(aboveFilter != null ? aboveFilter : box -> true)
                        .belowFilter(belowFilter != null ? belowFilter : box -> true)
                        .severity(severity)
                        .build()
        );
    }

    private void handleForbidAbove(Map<String, Object> effectJson,
                                   String ruleCode,
                                   RuleSeverity severity,
                                   Predicate<BoxInstance> filterA,
                                   Predicate<BoxInstance> filterB,
                                   ConstraintSet constraintSet) {
        String aboveSubject = (String) effectJson.get("above");
        String belowSubject = (String) effectJson.get("below");

        // Map subject labels to compiled predicates
        Predicate<BoxInstance> above;
        Predicate<BoxInstance> below;
        if ("packageA".equals(aboveSubject)) {
            above = filterA != null ? filterA : box -> true;
            below = filterB != null ? filterB : box -> true;
        } else {
            above = filterB != null ? filterB : box -> true;
            below = filterA != null ? filterA : box -> true;
        }

        constraintSet.getForbiddenPlacements().add(
                ForbiddenPlacement.builder()
                        .ruleCode(ruleCode)
                        .aboveFilter(above)
                        .belowFilter(below)
                        .severity(severity)
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private void handleSetConstraint(Map<String, Object> effectJson,
                                     String ruleCode,
                                     RuleSeverity severity,
                                     ConstraintSet constraintSet) {
        String constraint = (String) effectJson.get("constraint");
        BigDecimal value = toBigDecimal(effectJson.get("value"));
        String unit = (String) effectJson.get("unit");

        // Support filter from "supports" array or default to all
        Predicate<String> supportFilter;
        Object supports = effectJson.get("supports");
        if (supports instanceof List<?> codes) {
            List<String> codeList = codes.stream().map(String::valueOf).toList();
            supportFilter = codeList::contains;
        } else {
            supportFilter = code -> true;
        }

        constraintSet.getCapacityLimits().add(
                CapacityLimit.builder()
                        .ruleCode(ruleCode)
                        .supportFilter(supportFilter)
                        .constraint(constraint)
                        .value(value)
                        .unit(unit)
                        .severity(severity)
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private void handleSupportRule(Map<String, Object> effectJson,
                                   String ruleCode,
                                   RuleSeverity severity,
                                   SupportRuleType type,
                                   ConstraintSet constraintSet) {
        List<String> codes;
        if (effectJson.containsKey("supports")) {
            codes = ((List<?>) effectJson.get("supports")).stream()
                    .map(String::valueOf)
                    .toList();
        } else if (effectJson.containsKey("support")) {
            codes = List.of(String.valueOf(effectJson.get("support")));
        } else {
            codes = List.of();
        }

        constraintSet.getSupportRules().add(
                SupportRule.builder()
                        .ruleCode(ruleCode)
                        .type(type)
                        .supportCodes(codes)
                        .severity(severity)
                        .build()
        );
    }

    private void addObjective(String ruleCode,
                              ObjectiveType objectiveType,
                              Map<String, Object> effectJson,
                              ConstraintSet constraintSet) {
        BigDecimal weight = effectJson.containsKey("weight")
                ? toBigDecimal(effectJson.get("weight"))
                : BigDecimal.ONE;
        String attribute = (String) effectJson.get("attribute");

        constraintSet.getOptimizationObjectives().add(
                OptimizationObjective.builder()
                        .ruleCode(ruleCode)
                        .type(objectiveType)
                        .weight(weight)
                        .attribute(attribute)
                        .build()
        );
    }

    // ------------------------------------------------------------------ util

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return new BigDecimal(num.toString());
        if (obj instanceof String s) return new BigDecimal(s);
        return BigDecimal.ZERO;
    }
}
