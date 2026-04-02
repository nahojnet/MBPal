package com.mbpal.repository;

import com.mbpal.domain.entity.PalletizationExecution;
import com.mbpal.domain.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PalletizationExecutionRepository extends JpaRepository<PalletizationExecution, Long> {

    Optional<PalletizationExecution> findByExecutionCode(String code);

    List<PalletizationExecution> findByOrder_ExternalOrderId(String orderId);

    Page<PalletizationExecution> findByOrder_CustomerIdAndStatusIn(String customerId, List<ExecutionStatus> statuses, Pageable pageable);

    Page<PalletizationExecution> findByStatusAndCreatedAtBetween(ExecutionStatus status, Instant from, Instant to, Pageable pageable);
}
