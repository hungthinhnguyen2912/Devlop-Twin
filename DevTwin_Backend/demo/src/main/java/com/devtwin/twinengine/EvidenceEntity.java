package com.devtwin.twinengine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidence")
public class EvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_claim_id", nullable = false)
    private SkillClaimEntity skillClaim;

    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType;

    @Column(name = "repo_full_name", nullable = false, length = 255)
    private String repoFullName;

    @Column(nullable = false, columnDefinition = "text")
    private String detail;

    @Column(columnDefinition = "text")
    private String url;

    protected EvidenceEntity() {
    }

    public EvidenceEntity(SkillClaimEntity skillClaim, Evidence evidence) {
        this.skillClaim = skillClaim;
        this.evidenceType = evidence.evidenceType();
        this.repoFullName = evidence.repoFullName();
        this.detail = evidence.detail();
        this.url = evidence.url();
    }

    public Evidence toDto() {
        return new Evidence(evidenceType, repoFullName, detail, url);
    }
}
