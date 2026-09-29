package com.devtwin.analyzer;

import com.devtwin.connector.RawData;
import com.devtwin.connector.RawDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RepositoryAnalyzer {

    private static final String PLATFORM = "github";

    private final RawDataRepository rawDataRepository;
    private final NormalizedRepositoryRepository normalizedRepository;
    private final TechDetector techDetector;

    public RepositoryAnalyzer(
            RawDataRepository rawDataRepository,
            NormalizedRepositoryRepository normalizedRepository,
            TechDetector techDetector
    ) {
        this.rawDataRepository = rawDataRepository;
        this.normalizedRepository = normalizedRepository;
        this.techDetector = techDetector;
    }

    @Transactional
    public AnalyzeResult analyze(String username) {
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        List<RawData> rawData = rawDataRepository.findAllByPlatformAndUsername(PLATFORM, normalizedUsername);
        List<RawData> repositoryRows = rawData.stream()
                .filter(item -> "repo".equals(item.getDataType()))
                .toList();

        if (repositoryRows.isEmpty()) {
            throw new AnalyzerInputException(
                    "No repository data found for '" + normalizedUsername
                            + "'. Run POST /api/ingest/" + normalizedUsername + " first."
            );
        }

        Map<String, JsonNode> languagesByRepository = new HashMap<>();
        Map<String, List<DependencyFile>> filesByRepository = new HashMap<>();
        rawData.forEach(item -> collectSupportingData(item, languagesByRepository, filesByRepository));

        List<NormalizedRepo> analyzedRepositories = new ArrayList<>();
        int skippedForks = 0;
        for (RawData repositoryRow : repositoryRows) {
            JsonNode payload = repositoryRow.getPayload();
            if (payload.path("fork").booleanValue(false)) {
                skippedForks++;
                continue;
            }

            String repositoryName = payload.path("name").stringValue(repositoryRow.getRef());
            List<DetectedTech> technologies = techDetector.detect(
                    languagesByRepository.get(repositoryName),
                    filesByRepository.getOrDefault(repositoryName, List.of())
            );
            analyzedRepositories.add(new NormalizedRepo(
                    repositoryName,
                    payload.path("full_name").stringValue(normalizedUsername + "/" + repositoryName),
                    nullableText(payload.path("description")),
                    nullableText(payload.path("language")),
                    false,
                    payload.path("stargazers_count").intValue(0),
                    instant(payload.path("created_at")),
                    instant(payload.path("pushed_at")),
                    technologies
            ));
        }

        Instant analyzedAt = Instant.now();
        normalizedRepository.deleteSnapshot(PLATFORM, normalizedUsername);
        normalizedRepository.saveAll(analyzedRepositories.stream()
                .map(repository -> new NormalizedRepositoryEntity(
                        PLATFORM,
                        normalizedUsername,
                        repository,
                        analyzedAt
                ))
                .toList());

        int technologyCount = analyzedRepositories.stream()
                .mapToInt(repository -> repository.technologies().size())
                .sum();
        return new AnalyzeResult(
                PLATFORM,
                normalizedUsername,
                repositoryRows.size(),
                analyzedRepositories.size(),
                skippedForks,
                technologyCount,
                analyzedAt,
                analyzedRepositories
        );
    }

    private void collectSupportingData(
            RawData item,
            Map<String, JsonNode> languagesByRepository,
            Map<String, List<DependencyFile>> filesByRepository
    ) {
        if ("languages".equals(item.getDataType())) {
            languagesByRepository.put(item.getRef(), item.getPayload());
            return;
        }
        if (!"dependency_file".equals(item.getDataType())) {
            return;
        }

        int separator = item.getRef().indexOf('/');
        if (separator < 1 || separator == item.getRef().length() - 1) {
            return;
        }
        String repositoryName = item.getRef().substring(0, separator);
        String path = item.getPayload().path("path").stringValue(item.getRef().substring(separator + 1));
        String content = item.getPayload().path("content").stringValue("");
        filesByRepository.computeIfAbsent(repositoryName, ignored -> new ArrayList<>())
                .add(new DependencyFile(path, content));
    }

    private String nullableText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.stringValue("");
        return value.isBlank() ? null : value;
    }

    private Instant instant(JsonNode node) {
        String value = node.stringValue("");
        if (value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
