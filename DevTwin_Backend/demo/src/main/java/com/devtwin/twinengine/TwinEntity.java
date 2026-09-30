package com.devtwin.twinengine;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "twin",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_twin",
                columnNames = {"platform", "username"}
        )
)
public class TwinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode profile;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Column(name = "summary_source_hash", length = 64)
    private String summarySourceHash;

    @Column(name = "built_at", nullable = false)
    private Instant builtAt;

    @OneToMany(mappedBy = "twin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<SkillClaimEntity> skills = new ArrayList<>();

    protected TwinEntity() {
    }

    public TwinEntity(
            String platform,
            String username,
            String displayName,
            JsonNode profile,
            Instant builtAt,
            String aiSummary,
            String summarySourceHash,
            List<SkillClaim> skills
    ) {
        this.platform = platform;
        this.username = username;
        this.displayName = displayName;
        this.profile = profile;
        this.builtAt = builtAt;
        this.aiSummary = aiSummary;
        this.summarySourceHash = summarySourceHash;
        skills.forEach(skill -> this.skills.add(new SkillClaimEntity(this, skill)));
    }

    public String getPlatform() {
        return platform;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public JsonNode getProfile() {
        return profile;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public Instant getBuiltAt() {
        return builtAt;
    }

    public String getSummarySourceHash() {
        return summarySourceHash;
    }

    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public List<SkillClaimEntity> getSkills() {
        return skills;
    }
}
