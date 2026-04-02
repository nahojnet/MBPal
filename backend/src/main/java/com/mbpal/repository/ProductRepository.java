package com.mbpal.repository;

import com.mbpal.domain.entity.Product;
import com.mbpal.domain.enums.TemperatureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCode(String code);

    Page<Product> findByActiveFlag(String flag, Pageable pageable);

    Page<Product> findByTemperatureType(TemperatureType type, Pageable pageable);

    List<Product> findByProductCodeIn(List<String> codes);
}
