package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "RULE_PRIORITY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RulePriority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RULE_PRIORITY_ID")
    private Long rulePriorityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULESET_ID", nullable = false)
    private Ruleset ruleset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_ID", nullable = false)
    private Rule rule;

    @Column(name = "PRIORITY_ORDER", nullable = false)
    private Integer priorityOrder;

    @Column(name = "WEIGHT", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal weight = new BigDecimal("50.00");
}
