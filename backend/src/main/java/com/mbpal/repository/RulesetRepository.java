package com.mbpal.repository;

import com.mbpal.domain.entity.Ruleset;
import com.mbpal.domain.enums.RuleVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RulesetRepository extends JpaRepository<Ruleset, Long> {

    Optional<Ruleset> findByRulesetCode(String code);

    List<Ruleset> findByStatus(RuleVersionStatus status);
}
