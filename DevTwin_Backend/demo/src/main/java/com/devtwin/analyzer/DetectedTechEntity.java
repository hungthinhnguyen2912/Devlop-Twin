package com.devtwin.analyzer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "detected_tech",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_detected_tech",
                columnNames = {"repo_id", "tech_name", "source"}
        )
)
public class DetectedTechEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repo_id", nullable = false)
    private NormalizedRepositoryEntity repository;

    @Column(name = "tech_name", nullable = false, length = 100)
    private String techName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(columnDefinition = "text")
    private String detail;

    protected DetectedTechEntity() {
    }

    public DetectedTechEntity(NormalizedRepositoryEntity repository, DetectedTech technology) {
        this.repository = repository;
        this.techName = technology.name();
        this.category = technology.category();
        this.source = technology.source();
        this.detail = technology.detail();
    }

    public DetectedTech toDto() {
        return new DetectedTech(techName, category, source, detail);
    }
}
