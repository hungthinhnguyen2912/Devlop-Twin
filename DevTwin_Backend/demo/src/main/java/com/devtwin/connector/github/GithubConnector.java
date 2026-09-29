package com.devtwin.connector.github;

import com.devtwin.connector.Connector;
import com.devtwin.connector.RawDataStore;
import com.devtwin.connector.RawFetchResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GithubConnector implements Connector {

    private static final String PLATFORM = "github";
    private static final Duration CACHE_DURATION = Duration.ofHours(24);
    private static final Set<String> DEPENDENCY_FILES = Set.of(
            "package.json",
            "pom.xml",
            "build.gradle",
            "build.gradle.kts",
            "requirements.txt",
            "go.mod",
            "Dockerfile",
            "docker-compose.yml",
            "docker-compose.yaml",
            "compose.yml",
            "compose.yaml"
    );

    private final GithubApiClient githubApiClient;
    private final RawDataStore rawDataStore;
    private final ObjectMapper objectMapper;

    public GithubConnector(
            GithubApiClient githubApiClient,
            RawDataStore rawDataStore,
            ObjectMapper objectMapper
    ) {
        this.githubApiClient = githubApiClient;
        this.rawDataStore = rawDataStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public RawFetchResult fetch(String username) {
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        if (rawDataStore.isFresh(PLATFORM, normalizedUsername, "ingest_summary", "", CACHE_DURATION)) {
            return cachedResult(normalizedUsername);
        }

        List<String> warnings = new ArrayList<>();
        JsonNode profile = githubApiClient.getUser(normalizedUsername);
        rawDataStore.upsert(PLATFORM, normalizedUsername, "profile", "", profile);

        List<JsonNode> repositories = githubApiClient.listRepositories(normalizedUsername);
        int languageCount = 0;
        int readmeCount = 0;
        int dependencyFileCount = 0;
        int commitActivityCount = 0;

        for (JsonNode repository : repositories) {
            String repositoryName = repository.path("name").stringValue("");
            String owner = repository.path("owner").path("login").stringValue(normalizedUsername);
            rawDataStore.upsert(PLATFORM, normalizedUsername, "repo", repositoryName, repository);

            try {
                if (!rawDataStore.isFresh(
                        PLATFORM, normalizedUsername, "languages", repositoryName, CACHE_DURATION
                )) {
                    JsonNode languages = githubApiClient.getLanguages(owner, repositoryName);
                    rawDataStore.upsert(PLATFORM, normalizedUsername, "languages", repositoryName, languages);
                }
                languageCount++;
            } catch (GithubRateLimitException exception) {
                throw exception;
            } catch (GithubApiException exception) {
                warnings.add(repositoryName + "/languages: " + exception.getMessage());
            }

            try {
                if (rawDataStore.isFresh(
                        PLATFORM, normalizedUsername, "readme", repositoryName, CACHE_DURATION
                )) {
                    readmeCount++;
                } else {
                    var readme = githubApiClient.getReadme(owner, repositoryName);
                    if (readme.isPresent()) {
                        rawDataStore.upsert(
                                PLATFORM,
                                normalizedUsername,
                                "readme",
                                repositoryName,
                                decodedContent(readme.get())
                        );
                        readmeCount++;
                    }
                }
            } catch (GithubRateLimitException exception) {
                throw exception;
            } catch (GithubApiException | IllegalArgumentException exception) {
                warnings.add(repositoryName + "/README: " + exception.getMessage());
            }

            try {
                var rootContents = githubApiClient.getRootContents(owner, repositoryName);
                if (rootContents.isPresent()) {
                    for (String path : dependencyFilesIn(rootContents.get())) {
                        String ref = repositoryName + "/" + path;
                        if (rawDataStore.isFresh(
                                PLATFORM, normalizedUsername, "dependency_file", ref, CACHE_DURATION
                        )) {
                            dependencyFileCount++;
                        } else {
                            var file = githubApiClient.getFileContent(owner, repositoryName, path);
                            if (file.isPresent()) {
                                rawDataStore.upsert(
                                        PLATFORM,
                                        normalizedUsername,
                                        "dependency_file",
                                        ref,
                                        decodedContent(file.get())
                                );
                                dependencyFileCount++;
                            }
                        }
                    }
                }
            } catch (GithubRateLimitException exception) {
                throw exception;
            } catch (GithubApiException | IllegalArgumentException exception) {
                warnings.add(repositoryName + "/dependencies: " + exception.getMessage());
            }

            try {
                if (rawDataStore.isFresh(
                        PLATFORM, normalizedUsername, "commit_activity", repositoryName, CACHE_DURATION
                )) {
                    commitActivityCount++;
                } else {
                    var commitActivity = githubApiClient.getCommitActivity(owner, repositoryName);
                    if (commitActivity.isPresent()) {
                        rawDataStore.upsert(
                                PLATFORM,
                                normalizedUsername,
                                "commit_activity",
                                repositoryName,
                                commitActivity.get()
                        );
                        commitActivityCount++;
                    }
                }
            } catch (GithubRateLimitException exception) {
                throw exception;
            } catch (GithubApiException exception) {
                warnings.add(repositoryName + "/commit-activity: " + exception.getMessage());
            }
        }

        Instant completedAt = Instant.now();
        RawFetchResult result = new RawFetchResult(
                PLATFORM,
                normalizedUsername,
                false,
                repositories.size(),
                languageCount,
                readmeCount,
                dependencyFileCount,
                commitActivityCount,
                warnings,
                completedAt
        );
        rawDataStore.upsert(
                PLATFORM,
                normalizedUsername,
                "ingest_summary",
                "",
                objectMapper.valueToTree(result)
        );
        return result;
    }

    private RawFetchResult cachedResult(String username) {
        return new RawFetchResult(
                PLATFORM,
                username,
                true,
                toInt(rawDataStore.count(PLATFORM, username, "repo")),
                toInt(rawDataStore.count(PLATFORM, username, "languages")),
                toInt(rawDataStore.count(PLATFORM, username, "readme")),
                toInt(rawDataStore.count(PLATFORM, username, "dependency_file")),
                toInt(rawDataStore.count(PLATFORM, username, "commit_activity")),
                List.of(),
                Instant.now()
        );
    }

    private List<String> dependencyFilesIn(JsonNode rootContents) {
        if (!rootContents.isArray()) {
            return List.of();
        }

        Map<String, String> actualPaths = new LinkedHashMap<>();
        rootContents.forEach(item -> {
            if ("file".equals(item.path("type").stringValue(""))) {
                String path = item.path("path").stringValue("");
                actualPaths.put(path.toLowerCase(Locale.ROOT), path);
            }
        });

        return DEPENDENCY_FILES.stream()
                .map(candidate -> actualPaths.get(candidate.toLowerCase(Locale.ROOT)))
                .filter(path -> path != null)
                .toList();
    }

    private ObjectNode decodedContent(JsonNode githubContent) {
        String encodedContent = githubContent.path("content").stringValue("").replaceAll("\\s", "");
        if (encodedContent.isBlank()) {
            throw new IllegalArgumentException("GitHub returned empty Base64 content");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
        String content = decodeText(decodedBytes).replace("\u0000", "");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("content", content);
        payload.put("path", githubContent.path("path").stringValue(""));
        payload.put("html_url", githubContent.path("html_url").stringValue(""));
        return payload;
    }

    private String decodeText(byte[] bytes) {
        boolean hasUtf16Bom = bytes.length >= 2
                && ((bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
                    || (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF));
        boolean looksLikeUtf16LittleEndian = bytes.length >= 4 && bytes[1] == 0 && bytes[3] == 0;
        if (hasUtf16Bom) {
            return new String(bytes, StandardCharsets.UTF_16);
        }
        if (looksLikeUtf16LittleEndian) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private int toInt(long value) {
        return Math.toIntExact(value);
    }
}
