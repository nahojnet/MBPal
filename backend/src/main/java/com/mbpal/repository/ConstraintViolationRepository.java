package com.mbpal.repository;

import com.mbpal.domain.entity.ConstraintViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConstraintViolationRepository extends JpaRepository<ConstraintViolation, Long> {

    List<ConstraintViolation> findByExecution_ExecutionId(Long executionId);
}
