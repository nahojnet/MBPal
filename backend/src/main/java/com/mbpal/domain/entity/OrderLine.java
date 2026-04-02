package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ORDER_LINE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_LINE_ID")
    private Long orderLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(name = "BOX_QUANTITY", nullable = false)
    private Integer boxQuantity;

    @Column(name = "LINE_NUMBER", nullable = false)
    private Integer lineNumber;
}
