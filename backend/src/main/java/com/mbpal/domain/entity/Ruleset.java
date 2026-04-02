package com.mbpal.domain.entity;

import com.mbpal.domain.enums.RuleVersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RULESET")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruleset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RULESET_ID")
    private Long rulesetId;

    @Column(name = "RULESET_CODE", nullable = false, unique = true, length = 50)
    private String rulesetCode;

    @Column(name = "LABEL", nullable = false, length = 200)
    private String label;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private RuleVersionStatus status = RuleVersionStatus.DRAFT;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "PUBLISHED_AT")
    private Instant publishedAt;

    @Column(name = "ARCHIVED_AT")
    private Instant archivedAt;

    @OneToMany(mappedBy = "ruleset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RulesetRule> rulesetRules = new ArrayList<>();

    @OneToMany(mappedBy = "ruleset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RulePriority> priorities = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
