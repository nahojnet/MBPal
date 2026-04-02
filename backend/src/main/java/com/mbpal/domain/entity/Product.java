package com.mbpal.domain.entity;

import com.mbpal.domain.enums.FragilityLevel;
import com.mbpal.domain.enums.TemperatureType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "PRODUCT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "PRODUCT_CODE", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "LABEL", nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "TEMPERATURE_TYPE", nullable = false, length = 20)
    private TemperatureType temperatureType;

    @Column(name = "LENGTH_MM", nullable = false)
    private Integer lengthMm;

    @Column(name = "WIDTH_MM", nullable = false)
    private Integer widthMm;

    @Column(name = "HEIGHT_MM", nullable = false)
    private Integer heightMm;

    @Column(name = "WEIGHT_KG", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "FRAGILITY_LEVEL", nullable = false, length = 20)
    @Builder.Default
    private FragilityLevel fragilityLevel = FragilityLevel.ROBUSTE;

    @Column(name = "STACKABLE_FLAG", nullable = false, length = 1)
    @Builder.Default
    private String stackableFlag = "Y";

    @Column(name = "MAX_STACK_WEIGHT_KG", precision = 8, scale = 2)
    private BigDecimal maxStackWeightKg;

    @Column(name = "ORIENTATION_FIXED", nullable = false, length = 1)
    @Builder.Default
    private String orientationFixed = "N";

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

    public boolean isStackable() {
        return "Y".equals(stackableFlag);
    }

    public boolean isActive() {
        return "Y".equals(activeFlag);
    }
}
