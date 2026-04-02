package com.mbpal.domain.entity;

import com.mbpal.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CUSTOMER_ORDER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "EXTERNAL_ORDER_ID", nullable = false, unique = true, length = 50)
    private String externalOrderId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "CUSTOMER_NAME", length = 200)
    private String customerName;

    @Column(name = "WAREHOUSE_CODE", length = 20)
    private String warehouseCode;

    @Column(name = "ORDER_DATE")
    private LocalDate orderDate;

    @Column(name = "RECEIVED_AT", nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.RECEIVED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        receivedAt = Instant.now();
    }
}
