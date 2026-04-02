package com.mbpal.domain.entity;

import com.mbpal.domain.enums.RuleSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "CONSTRAINT_VIOLATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstraintViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VIOLATION_ID")
    private Long violationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXECUTION_ID", nullable = false)
    private PalletizationExecution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PALLET_ID")
    private Pallet pallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_VERSION_ID", nullable = false)
    private RuleVersion ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", nullable = false, length = 10)
    private RuleSeverity severity;

    @Column(name = "DESCRIPTION", nullable = false, length = 1000)
    private String description;

    @Column(name = "IMPACT_SCORE", precision = 5, scale = 2)
    private BigDecimal impactScore;
}
