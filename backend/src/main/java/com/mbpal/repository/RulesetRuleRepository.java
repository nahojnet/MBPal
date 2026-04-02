package com.mbpal.repository;

import com.mbpal.domain.entity.RulesetRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RulesetRuleRepository extends JpaRepository<RulesetRule, Long> {

    List<RulesetRule> findByRuleset_RulesetId(Long rulesetId);
}
