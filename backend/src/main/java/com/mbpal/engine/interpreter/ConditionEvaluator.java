package com.mbpal.engine.interpreter;

import com.mbpal.engine.model.BoxInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Compiles a JSON condition (parsed as Map/List) into a {@link Predicate} over {@link BoxInstance}.
 * Supports logical combinators (all, any, not) and field-level operators.
 */
@Slf4j
@Component
public class ConditionEvaluator {

    // ------------------------------------------------------------------ public API

    /**
     * Compile a single-box condition (PACKAGE or PALLET scope).
     */
    public Predicate<BoxInstance> compileCondition(Map<String, Object> conditionJson) {
        if (conditionJson == null || conditionJson.isEmpty()) {
            return box -> true;
        }
        return compileNode(conditionJson);
    }

    /**
     * Compile an INTER_PACKAGE condition that references "packageA" and "packageB" subjects.
     * Returns a pair of predicates: key = packageA filter, value = packageB filter.
     */
    public Map.Entry<Predicate<BoxInstance>, Predicate<BoxInstance>> compileInterPackageCondition(
            Map<String, Object> conditionJson) {

        if (conditionJson == null || conditionJson.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(box -> true, box -> true);
        }

        Predicate<BoxInstance> filterA = box -> true;
        Predicate<BoxInstance> filterB = box -> true;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clauses = extractClauses(conditionJson);

        for (Map<String, Object> clause : clauses) {
            String subject = (String) clause.get("subject");
            Predicate<BoxInstance> compiled = compileFieldPredicate(clause);
            if ("packageA".equals(subject)) {
                filterA = filterA.and(compiled);
            } else if ("packageB".equals(subject)) {
                filterB = filterB.and(compiled);
            } else {
                log.warn("INTER_PACKAGE condition clause has unrecognized subject '{}'; skipping", subject);
            }
        }
        return new AbstractMap.SimpleEntry<>(filterA, filterB);
    }

    // ------------------------------------------------------------------ internal

    @SuppressWarnings("unchecked")
    private Predicate<BoxInstance> compileNode(Map<String, Object> node) {
        if (node.containsKey("all")) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("all");
            return children.stream()
                    .map(this::compileNode)
                    .reduce(Predicate::and)
                    .orElse(box -> true);
        }
        if (node.containsKey("any")) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("any");
            return children.stream()
                    .map(this::compileNode)
                    .reduce(Predicate::or)
                    .orElse(box -> false);
        }
        if (node.containsKey("not")) {
            Object inner = node.get("not");
            if (inner instanceof Map) {
                return compileNode((Map<String, Object>) inner).negate();
            }
            log.warn("'not' node contains unexpected type: {}", inner == null ? "null" : inner.getClass());
            return box -> true;
        }

        // Leaf: field / operator / value
        if (node.containsKey("field")) {
            return compileFieldPredicate(node);
        }

        log.warn("Unrecognized condition node keys: {}", node.keySet());
        return box -> true;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractClauses(Map<String, Object> conditionJson) {
        if (conditionJson.containsKey("all")) {
            return (List<Map<String, Object>>) conditionJson.get("all");
        }
        if (conditionJson.containsKey("any")) {
            return (List<Map<String, Object>>) conditionJson.get("any");
        }
        // Single clause wrapped in a map
        return List.of(conditionJson);
    }

    // ------------------------------------------------------------------ field resolution

    private Predicate<BoxInstance> compileFieldPredicate(Map<String, Object> leaf) {
        String field = (String) leaf.get("field");
        String operator = (String) leaf.get("operator");
        Object value = leaf.get("value");

        Function<BoxInstance, Object> getter = resolveGetter(field);
        if (getter == null) {
            log.warn("Unrecognized condition field '{}'; predicate will always be true", field);
            return box -> true;
        }

        return box -> {
            try {
                Object actual = getter.apply(box);
                return evaluateOperator(actual, operator, value);
            } catch (Exception e) {
                log.warn("Error evaluating condition field='{}' operator='{}': {}", field, operator, e.getMessage());
                return false;
            }
        };
    }

    private Function<BoxInstance, Object> resolveGetter(String field) {
        if (field == null) return null;
        return switch (field) {
            case "weight_kg" -> box -> box.getWeightKg();
            case "height_mm" -> box -> box.getHeightMm();
            case "length_mm" -> box -> box.getLengthMm();
            case "width_mm" -> box -> box.getWidthMm();
            case "temperature_type" -> box -> box.getTemperatureType() != null ? box.getTemperatureType().name() : null;
            case "fragility_level" -> box -> box.getFragilityLevel() != null ? box.getFragilityLevel().name() : null;
            case "stackable_flag" -> box -> box.isStackable();
            case "max_stack_weight_kg" -> box -> box.getMaxStackWeightKg();
            case "product_code" -> box -> box.getProductCode();
            case "stacking_class" -> box -> box.getStackingClass();
            case "temperature_group" -> box -> box.getTemperatureGroup();
            default -> {
                log.warn("Unknown field mapping: '{}'", field);
                yield null;
            }
        };
    }

    // ------------------------------------------------------------------ operator evaluation

    @SuppressWarnings("unchecked")
    private boolean evaluateOperator(Object actual, String operator, Object expected) {
        if (operator == null) {
            log.warn("Null operator in condition; defaulting to true");
            return true;
        }
        return switch (operator) {
            case "=" -> isEqual(actual, expected);
            case "!=" -> !isEqual(actual, expected);
            case ">" -> compareNumeric(actual, expected) > 0;
            case ">=" -> compareNumeric(actual, expected) >= 0;
            case "<" -> compareNumeric(actual, expected) < 0;
            case "<=" -> compareNumeric(actual, expected) <= 0;
            case "IN" -> evaluateIn(actual, expected);
            case "NOT_IN" -> !evaluateIn(actual, expected);
            case "BETWEEN" -> evaluateBetween(actual, expected);
            case "IS_NULL" -> actual == null;
            case "IS_NOT_NULL" -> actual != null;
            default -> {
                log.warn("Unrecognized operator '{}'; defaulting to true", operator);
                yield true;
            }
        };
    }

    private boolean isEqual(Object actual, Object expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        // Numeric comparison (handle int vs BigDecimal, etc.)
        if (actual instanceof Number && expected instanceof Number) {
            return toBigDecimal(actual).compareTo(toBigDecimal(expected)) == 0;
        }
        if (actual instanceof Boolean && expected instanceof Boolean) {
            return actual.equals(expected);
        }
        // String comparison
        return String.valueOf(actual).equals(String.valueOf(expected));
    }

    private int compareNumeric(Object actual, Object expected) {
        return toBigDecimal(actual).compareTo(toBigDecimal(expected));
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateIn(Object actual, Object expected) {
        if (actual == null) return false;
        if (expected instanceof Collection<?> col) {
            String actualStr = String.valueOf(actual);
            return col.stream().anyMatch(v -> actualStr.equals(String.valueOf(v)));
        }
        return isEqual(actual, expected);
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateBetween(Object actual, Object expected) {
        if (!(expected instanceof List<?> range) || range.size() != 2) {
            log.warn("BETWEEN operator requires a list of exactly 2 values; got: {}", expected);
            return false;
        }
        BigDecimal val = toBigDecimal(actual);
        BigDecimal low = toBigDecimal(range.get(0));
        BigDecimal high = toBigDecimal(range.get(1));
        return val.compareTo(low) >= 0 && val.compareTo(high) <= 0;
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return new BigDecimal(num.toString());
        if (obj instanceof String s) return new BigDecimal(s);
        throw new IllegalArgumentException("Cannot convert to BigDecimal: " + obj);
    }
}
