package com.mbpal.domain.entity;

import com.mbpal.domain.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PALLETIZATION_EXECUTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PalletizationExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXECUTION_ID")
    private Long executionId;

    @Column(name = "EXECUTION_CODE", nullable = false, unique = true, length = 50)
    private String executionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULESET_ID", nullable = false)
    private Ruleset ruleset;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "DRY_RUN_FLAG", nullable = false, length = 1)
    @Builder.Default
    private String dryRunFlag = "N";

    @Column(name = "STARTED_AT")
    private Instant startedAt;

    @Column(name = "ENDED_AT")
    private Instant endedAt;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "TOTAL_PALLETS")
    private Integer totalPallets;

    @Column(name = "TOTAL_BOXES")
    private Integer totalBoxes;

    @Column(name = "GLOBAL_SCORE", precision = 5, scale = 2)
    private BigDecimal globalScore;

    @Column(name = "ERROR_MESSAGE", length = 2000)
    private String errorMessage;

    @Column(name = "EXECUTION_PARAMS", columnDefinition = "CLOB")
    private String executionParams;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Pallet> pallets = new ArrayList<>();

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConstraintViolation> violations = new ArrayList<>();

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DecisionTrace> traces = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isDryRun() {
        return "Y".equals(dryRunFlag);
    }
}
