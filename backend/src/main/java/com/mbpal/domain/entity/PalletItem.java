package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PALLET_ITEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PalletItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PALLET_ITEM_ID")
    private Long palletItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PALLET_ID", nullable = false)
    private Pallet pallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_LINE_ID", nullable = false)
    private OrderLine orderLine;

    @Column(name = "BOX_INSTANCE_INDEX", nullable = false)
    private Integer boxInstanceIndex;

    @Column(name = "LAYER_NO", nullable = false)
    private Integer layerNo;

    @Column(name = "POSITION_NO", nullable = false)
    private Integer positionNo;

    @Column(name = "STACKING_CLASS", length = 20)
    private String stackingClass;
}
