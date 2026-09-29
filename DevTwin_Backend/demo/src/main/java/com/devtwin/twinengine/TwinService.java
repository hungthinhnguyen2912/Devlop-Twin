package com.devtwin.twinengine;

import com.devtwin.analyzer.AnalyzerInputException;
import com.devtwin.analyzer.NormalizedRepo;
import com.devtwin.analyzer.NormalizedRepositoryEntity;
import com.devtwin.analyzer.NormalizedRepositoryRepository;
import com.devtwin.connector.RawData;
import com.devtwin.connector.RawDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TwinService {

    private static final String PLATFORM = "github";

    private final TwinEngine twinEngine;
    private final TwinRepository twinRepository;
    private final NormalizedRepositoryRepository normalizedRepository;
    private final RawDataRepository rawDataRepository;
    private final ObjectMapper objectMapper;

    public TwinService(
            TwinEngine twinEngine,
            TwinRepository twinRepository,
            NormalizedRepositoryRepository normalizedRepository,
            RawDataRepository rawDataRepository,
            ObjectMapper objectMapper
    ) {
        this.twinEngine = twinEngine;
        this.twinRepository = twinRepository;
        this.normalizedRepository = normalizedRepository;
        this.rawDataRepository = rawDataRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeveloperTwin build(String username, List<NormalizedRepo> repositories) {
        String normalizedUsername = normalize(username);
        if (repositories.isEmpty()) {
            throw new AnalyzerInputException(
                    "No normalized repositories found for '" + normalizedUsername + "'."
            );
        }

        JsonNode rawProfile = rawDataRepository
                .findByPlatformAndUsernameAndDataTypeAndRef(PLATFORM, normalizedUsername, "profile", "")
                .map(RawData::getPayload)
                .orElseThrow(() -> new AnalyzerInputException(
                        "No profile data found for '" + normalizedUsername
                                + "'. Run POST /api/ingest/" + normalizedUsername + " first."
                ));
        ObjectNode profile = normalizeProfile(rawProfile);
        String displayName = nullableText(rawProfile.path("name"));
        if (displayName == null) {
            displayName = normalizedUsername;
        }

        List<SkillClaim> claims = twinEngine.buildClaims(repositories);
        Instant builtAt = Instant.now();
        twinRepository.deleteSnapshot(PLATFORM, normalizedUsername);
        TwinEntity saved = twinRepository.save(new TwinEntity(
                PLATFORM,
                normalizedUsername,
                displayName,
                profile,
                builtAt,
                claims
        ));
        return response(saved, repositories);
    }

    @Transactional(readOnly = true)
    public DeveloperTwin get(String username) {
        String normalizedUsername = normalize(username);
        TwinEntity twin = twinRepository.findByPlatformAndUsername(PLATFORM, normalizedUsername)
                .orElseThrow(() -> new TwinNotFoundException(
                        "Twin not found for '" + normalizedUsername
                                + "'. Run POST /api/twin/" + normalizedUsername + "/build first."
                ));
        List<NormalizedRepo> repositories = normalizedRepository
                .findAllByPlatformAndUsername(PLATFORM, normalizedUsername)
                .stream()
                .map(NormalizedRepositoryEntity::toDto)
                .toList();
        return response(twin, repositories);
    }

    private DeveloperTwin response(TwinEntity twin, List<NormalizedRepo> repositories) {
        List<SkillClaim> skills = twin.getSkills().stream()
                .map(SkillClaimEntity::toDto)
                .sorted(Comparator.comparingInt(SkillClaim::repoCount).reversed()
                        .thenComparing(SkillClaim::techName))
                .toList();
        List<NormalizedRepo> topRepositories = repositories.stream()
                .sorted(Comparator.comparingInt(NormalizedRepo::stars).reversed()
                        .thenComparing(
                                NormalizedRepo::pushedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .limit(5)
                .toList();
        return new DeveloperTwin(
                twin.getPlatform(),
                twin.getUsername(),
                twin.getDisplayName(),
                twin.getProfile(),
                twin.getAiSummary(),
                twin.getBuiltAt(),
                skills,
                topRepositories
        );
    }

    private ObjectNode normalizeProfile(JsonNode rawProfile) {
        ObjectNode profile = objectMapper.createObjectNode();
        copyText(rawProfile, profile, "login", "login");
        copyText(rawProfile, profile, "name", "name");
        copyText(rawProfile, profile, "avatar_url", "avatarUrl");
        copyText(rawProfile, profile, "html_url", "htmlUrl");
        copyText(rawProfile, profile, "bio", "bio");
        copyText(rawProfile, profile, "location", "location");
        copyText(rawProfile, profile, "company", "company");
        copyText(rawProfile, profile, "blog", "blog");
        profile.put("followers", rawProfile.path("followers").intValue(0));
        profile.put("following", rawProfile.path("following").intValue(0));
        profile.put("publicRepos", rawProfile.path("public_repos").intValue(0));
        return profile;
    }

    private void copyText(JsonNode source, ObjectNode target, String sourceName, String targetName) {
        String value = nullableText(source.path(sourceName));
        if (value != null) {
            target.put(targetName, value);
        }
    }

    private String nullableText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.stringValue("");
        return value.isBlank() ? null : value;
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
