package com.mbpal.domain.entity;

import com.mbpal.domain.enums.RuleVersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "RULE_VERSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RULE_VERSION_ID")
    private Long ruleVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_ID", nullable = false)
    private Rule rule;

    @Column(name = "SEMANTIC_VERSION", nullable = false, length = 20)
    private String semanticVersion;

    @Column(name = "CONDITION_JSON", nullable = false, columnDefinition = "CLOB")
    private String conditionJson;

    @Column(name = "EFFECT_JSON", nullable = false, columnDefinition = "CLOB")
    private String effectJson;

    @Column(name = "EXPLANATION", nullable = false, length = 1000)
    private String explanation;

    @Column(name = "VALID_FROM")
    private Instant validFrom;

    @Column(name = "VALID_TO")
    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private RuleVersionStatus status = RuleVersionStatus.DRAFT;

    @Column(name = "PUBLISHED_BY", length = 100)
    private String publishedBy;

    @Column(name = "PUBLISHED_AT")
    private Instant publishedAt;

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
}
