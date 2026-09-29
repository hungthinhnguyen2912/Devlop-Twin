package com.devtwin.analyzer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "normalized_repo",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_normalized_repo",
                columnNames = {"platform", "full_name"}
        )
)
public class NormalizedRepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "primary_language", length = 100)
    private String primaryLanguage;

    @Column(name = "is_fork", nullable = false)
    private boolean fork;

    @Column(nullable = false)
    private int stars;

    @Column(name = "repo_created_at")
    private Instant repoCreatedAt;

    @Column(name = "last_pushed_at")
    private Instant lastPushedAt;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<DetectedTechEntity> technologies = new ArrayList<>();

    protected NormalizedRepositoryEntity() {
    }

    public NormalizedRepositoryEntity(String platform, String username, NormalizedRepo repository, Instant analyzedAt) {
        this.platform = platform;
        this.username = username;
        this.name = repository.name();
        this.fullName = repository.fullName();
        this.description = repository.description();
        this.primaryLanguage = repository.primaryLanguage();
        this.fork = repository.fork();
        this.stars = repository.stars();
        this.repoCreatedAt = repository.createdAt();
        this.lastPushedAt = repository.pushedAt();
        this.analyzedAt = analyzedAt;
        repository.technologies().forEach(technology -> technologies.add(new DetectedTechEntity(this, technology)));
    }

    public NormalizedRepo toDto() {
        return new NormalizedRepo(
                name,
                fullName,
                description,
                primaryLanguage,
                fork,
                stars,
                repoCreatedAt,
                lastPushedAt,
                technologies.stream().map(DetectedTechEntity::toDto).toList()
        );
    }
}
