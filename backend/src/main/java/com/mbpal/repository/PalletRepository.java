package com.mbpal.repository;

import com.mbpal.domain.entity.Pallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PalletRepository extends JpaRepository<Pallet, Long> {

    List<Pallet> findByExecution_ExecutionIdOrderByPalletNumberAsc(Long executionId);
}
