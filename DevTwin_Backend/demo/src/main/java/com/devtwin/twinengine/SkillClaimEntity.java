package com.devtwin.twinengine;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "skill_claim",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_skill_claim",
                columnNames = {"twin_id", "tech_name"}
        )
)
public class SkillClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "twin_id", nullable = false)
    private TwinEntity twin;

    @Column(name = "tech_name", nullable = false, length = 100)
    private String techName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "repo_count", nullable = false)
    private int repoCount;

    @Column(name = "first_used")
    private LocalDate firstUsed;

    @Column(name = "last_used")
    private LocalDate lastUsed;

    @OneToMany(mappedBy = "skillClaim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<EvidenceEntity> evidence = new ArrayList<>();

    protected SkillClaimEntity() {
    }

    public SkillClaimEntity(TwinEntity twin, SkillClaim skill) {
        this.twin = twin;
        this.techName = skill.techName();
        this.category = skill.category();
        this.repoCount = skill.repoCount();
        this.firstUsed = skill.firstUsed();
        this.lastUsed = skill.lastUsed();
        skill.evidence().forEach(item -> evidence.add(new EvidenceEntity(this, item)));
    }

    public SkillClaim toDto() {
        return new SkillClaim(
                techName,
                category,
                repoCount,
                firstUsed,
                lastUsed,
                evidence.stream().map(EvidenceEntity::toDto).toList()
        );
    }
}
