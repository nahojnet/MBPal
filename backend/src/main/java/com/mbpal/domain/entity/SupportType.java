package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "SUPPORT_TYPE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SUPPORT_TYPE_ID")
    private Long supportTypeId;

    @Column(name = "SUPPORT_CODE", nullable = false, unique = true, length = 30)
    private String supportCode;

    @Column(name = "LABEL", nullable = false, length = 100)
    private String label;

    @Column(name = "LENGTH_MM", nullable = false)
    private Integer lengthMm;

    @Column(name = "WIDTH_MM", nullable = false)
    private Integer widthMm;

    @Column(name = "HEIGHT_MM", nullable = false)
    private Integer heightMm;

    @Column(name = "MAX_LOAD_KG", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxLoadKg;

    @Column(name = "MAX_TOTAL_HEIGHT_MM", nullable = false)
    private Integer maxTotalHeightMm;

    @Column(name = "USABLE_LENGTH_MM")
    private Integer usableLengthMm;

    @Column(name = "USABLE_WIDTH_MM")
    private Integer usableWidthMm;

    @Column(name = "MERGEABLE_FLAG", nullable = false, length = 1)
    @Builder.Default
    private String mergeableFlag = "N";

    @Column(name = "MERGE_TARGET_CODE", length = 30)
    private String mergeTargetCode;

    @Column(name = "MERGE_QUANTITY")
    @Builder.Default
    private Integer mergeQuantity = 2;

    @Column(name = "ACTIVE_FLAG", nullable = false, length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isMergeable() {
        return "Y".equals(mergeableFlag);
    }

    public int getUsableLength() {
        return usableLengthMm != null ? usableLengthMm : lengthMm;
    }

    public int getUsableWidth() {
        return usableWidthMm != null ? usableWidthMm : widthMm;
    }
}
