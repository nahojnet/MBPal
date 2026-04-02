package com.mbpal.repository;

import com.mbpal.domain.entity.RuleVersion;
import com.mbpal.domain.enums.RuleVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleVersionRepository extends JpaRepository<RuleVersion, Long> {

    List<RuleVersion> findByRule_RuleCodeAndStatus(String ruleCode, RuleVersionStatus status);

    Optional<RuleVersion> findByRule_RuleIdAndSemanticVersion(Long ruleId, String version);
}
