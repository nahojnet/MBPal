package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PALLET")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PALLET_ID")
    private Long palletId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXECUTION_ID", nullable = false)
    private PalletizationExecution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUPPORT_TYPE_ID", nullable = false)
    private SupportType supportType;

    @Column(name = "PALLET_NUMBER", nullable = false)
    private Integer palletNumber;

    @Column(name = "TOTAL_WEIGHT_KG", precision = 8, scale = 2)
    private BigDecimal totalWeightKg;

    @Column(name = "TOTAL_HEIGHT_MM")
    private Integer totalHeightMm;

    @Column(name = "FILL_RATE_PCT", precision = 5, scale = 2)
    private BigDecimal fillRatePct;

    @Column(name = "STABILITY_SCORE", precision = 5, scale = 2)
    private BigDecimal stabilityScore;

    @Column(name = "LAYER_COUNT")
    private Integer layerCount;

    @Column(name = "BOX_COUNT")
    private Integer boxCount;

    @Column(name = "MERGED_FLAG", nullable = false, length = 1)
    @Builder.Default
    private String mergedFlag = "N";

    @OneToMany(mappedBy = "pallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PalletItem> items = new ArrayList<>();
}
