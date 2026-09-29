package com.devtwin.twinengine;

import com.devtwin.analyzer.DetectedTech;
import com.devtwin.analyzer.NormalizedRepo;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class TwinEngine {

    public List<SkillClaim> buildClaims(List<NormalizedRepo> repositories) {
        Map<String, ClaimAccumulator> claims = new LinkedHashMap<>();
        repositories.forEach(repository -> repository.technologies().forEach(technology -> claims
                .computeIfAbsent(
                        technology.name().toLowerCase(Locale.ROOT),
                        ignored -> new ClaimAccumulator(technology.name(), technology.category())
                )
                .add(repository, technology)));

        return claims.values().stream()
                .map(ClaimAccumulator::toClaim)
                .sorted(Comparator.comparingInt(SkillClaim::repoCount).reversed()
                        .thenComparing(SkillClaim::techName))
                .toList();
    }

    private static String evidenceType(String source) {
        return switch (source) {
            case "languages_api" -> "language";
            case "dockerfile", "docker_compose" -> "dockerfile";
            default -> "dependency";
        };
    }

    private static String evidenceUrl(String fullName, String source) {
        String repositoryUrl = "https://github.com/" + fullName;
        String path = switch (source) {
            case "maven" -> "pom.xml";
            case "gradle" -> "build.gradle";
            case "npm" -> "package.json";
            case "pip" -> "requirements.txt";
            case "go_mod" -> "go.mod";
            case "dockerfile" -> "Dockerfile";
            case "docker_compose" -> "docker-compose.yml";
            default -> null;
        };
        if (path == null) {
            return repositoryUrl;
        }
        return repositoryUrl + "/blob/HEAD/" + URLEncoder.encode(path, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static LocalDate date(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static LocalDate earlier(LocalDate current, LocalDate candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate == null || current.isBefore(candidate) ? current : candidate;
    }

    private static LocalDate later(LocalDate current, LocalDate candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate == null || current.isAfter(candidate) ? current : candidate;
    }

    private static final class ClaimAccumulator {

        private final String techName;
        private final String category;
        private final Set<String> repositories = new LinkedHashSet<>();
        private final List<Evidence> evidence = new ArrayList<>();
        private LocalDate firstUsed;
        private LocalDate lastUsed;

        private ClaimAccumulator(String techName, String category) {
            this.techName = techName;
            this.category = category;
        }

        private void add(NormalizedRepo repository, DetectedTech technology) {
            repositories.add(repository.fullName());
            firstUsed = earlier(firstUsed, date(repository.createdAt()));
            lastUsed = later(lastUsed, date(repository.pushedAt()));
            evidence.add(new Evidence(
                    evidenceType(technology.source()),
                    repository.fullName(),
                    technology.detail(),
                    evidenceUrl(repository.fullName(), technology.source())
            ));
        }

        private SkillClaim toClaim() {
            return new SkillClaim(
                    techName,
                    category,
                    repositories.size(),
                    firstUsed,
                    lastUsed,
                    evidence
            );
        }
    }
}
