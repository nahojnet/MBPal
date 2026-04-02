package com.mbpal.repository;

import com.mbpal.domain.entity.RulePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RulePriorityRepository extends JpaRepository<RulePriority, Long> {

    List<RulePriority> findByRuleset_RulesetIdOrderByPriorityOrderAsc(Long rulesetId);
}
