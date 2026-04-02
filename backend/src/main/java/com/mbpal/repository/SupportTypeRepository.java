package com.mbpal.repository;

import com.mbpal.domain.entity.SupportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTypeRepository extends JpaRepository<SupportType, Long> {

    Optional<SupportType> findBySupportCode(String code);

    List<SupportType> findByActiveFlag(String flag);

    List<SupportType> findBySupportCodeIn(List<String> codes);
}
