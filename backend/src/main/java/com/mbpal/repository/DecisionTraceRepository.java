package com.mbpal.repository;

import com.mbpal.domain.entity.DecisionTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionTraceRepository extends JpaRepository<DecisionTrace, Long> {

    List<DecisionTrace> findByExecution_ExecutionIdOrderByTraceOrderAsc(Long executionId);
}
