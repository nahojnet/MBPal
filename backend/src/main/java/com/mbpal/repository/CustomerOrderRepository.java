package com.mbpal.repository;

import com.mbpal.domain.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByExternalOrderId(String externalOrderId);

    Page<CustomerOrder> findByCustomerId(String customerId, Pageable pageable);
}
