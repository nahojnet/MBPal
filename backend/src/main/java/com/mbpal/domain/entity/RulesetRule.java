package com.mbpal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RULESET_RULE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RulesetRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RULESET_RULE_ID")
    private Long rulesetRuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULESET_ID", nullable = false)
    private Ruleset ruleset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_VERSION_ID", nullable = false)
    private RuleVersion ruleVersion;
}
