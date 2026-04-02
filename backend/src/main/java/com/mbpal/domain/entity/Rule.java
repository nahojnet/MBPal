package com.mbpal.domain.entity;

import com.mbpal.domain.enums.RuleScope;
import com.mbpal.domain.enums.RuleSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RULE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RULE_ID")
    private Long ruleId;

    @Column(name = "RULE_CODE", nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(name = "DOMAIN", nullable = false, length = 50)
    @Builder.Default
    private String domain = "PALLETIZATION";

    @Enumerated(EnumType.STRING)
    @Column(name = "SCOPE", nullable = false, length = 20)
    private RuleScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", nullable = false, length = 10)
    private RuleSeverity severity;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Column(name = "ACTIVE_FLAG", nullable = false, length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RuleVersion> versions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
