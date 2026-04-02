package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "DECISION_TRACE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRACE_ID")
    private Long traceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXECUTION_ID", nullable = false)
    private PalletizationExecution execution;

    @Column(name = "TRACE_ORDER", nullable = false)
    private Integer traceOrder;

    @Column(name = "STEP_NAME", nullable = false, length = 100)
    private String stepName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PALLET_ID")
    private Pallet pallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PALLET_ITEM_ID")
    private PalletItem palletItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_VERSION_ID")
    private RuleVersion ruleVersion;

    @Column(name = "DESCRIPTION", nullable = false, columnDefinition = "CLOB")
    private String description;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
