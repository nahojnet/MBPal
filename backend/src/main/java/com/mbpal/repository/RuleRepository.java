package com.mbpal.repository;

import com.mbpal.domain.entity.Rule;
import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    Optional<Rule> findByRuleCode(String code);

    List<Rule> findByActiveFlag(String flag);

    List<Rule> findByScopeAndActiveFlag(RuleScope scope, String flag);

    List<Rule> findBySeverityAndActiveFlag(RuleSeverity severity, String flag);
}
