package com.devtwin.connector.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GithubApiClient {

    private static final String ACCEPT = "application/vnd.github+json";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final int lowRateLimitThreshold;
    private final int statsRetries;
    private final Duration statsRetryDelay;

    public GithubApiClient(
            ObjectMapper objectMapper,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.version}") String apiVersion,
            @Value("${github.api.token:}") String token,
            @Value("${github.api.low-rate-limit-threshold:5}") int lowRateLimitThreshold,
            @Value("${github.api.stats-retries:3}") int statsRetries,
            @Value("${github.api.stats-retry-delay:1s}") Duration statsRetryDelay
    ) {
        this.objectMapper = objectMapper;
        this.lowRateLimitThreshold = lowRateLimitThreshold;
        this.statsRetries = statsRetries;
        this.statsRetryDelay = statsRetryDelay;
        this.restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT)
                .defaultHeader(HttpHeaders.USER_AGENT, "Developer-Twin")
                .defaultHeader("X-GitHub-Api-Version", apiVersion)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(token)) {
                        headers.setBearerAuth(token);
                    }
                })
                .build();
    }

    public JsonNode getUser(String username) {
        return requireSuccess(execute(restClient.get().uri("/users/{username}", username)), "GitHub user");
    }

    public List<JsonNode> listRepositories(String username) {
        List<JsonNode> repositories = new ArrayList<>();
        int page = 1;

        while (true) {
            int requestedPage = page;
            ApiResponse response = execute(restClient.get().uri(uriBuilder -> uriBuilder
                    .path("/users/{username}/repos")
                    .queryParam("type", "owner")
                    .queryParam("sort", "updated")
                    .queryParam("direction", "desc")
                    .queryParam("per_page", 100)
                    .queryParam("page", requestedPage)
                    .build(username)));
            JsonNode payload = requireSuccess(response, "GitHub repositories page " + requestedPage);

            if (!payload.isArray()) {
                throw new GithubApiException(502, "GitHub repositories response is not an array");
            }

            payload.forEach(repositories::add);
            if (payload.size() < 100) {
                return repositories;
            }
            page++;
        }
    }

    public JsonNode getLanguages(String owner, String repository) {
        return requireSuccess(
                execute(restClient.get().uri("/repos/{owner}/{repository}/languages", owner, repository)),
                "languages for " + owner + "/" + repository
        );
    }

    public Optional<JsonNode> getReadme(String owner, String repository) {
        return optionalContent(
                execute(restClient.get().uri("/repos/{owner}/{repository}/readme", owner, repository)),
                "README for " + owner + "/" + repository
        );
    }

    public Optional<JsonNode> getRootContents(String owner, String repository) {
        return optionalContent(
                execute(restClient.get().uri("/repos/{owner}/{repository}/contents", owner, repository)),
                "root contents for " + owner + "/" + repository
        );
    }

    public Optional<JsonNode> getFileContent(String owner, String repository, String path) {
        return optionalContent(
                execute(restClient.get().uri(
                        "/repos/{owner}/{repository}/contents/{path}",
                        owner,
                        repository,
                        path
                )),
                path + " for " + owner + "/" + repository
        );
    }

    public Optional<JsonNode> getCommitActivity(String owner, String repository) {
        for (int attempt = 1; attempt <= statsRetries; attempt++) {
            ApiResponse response = execute(restClient.get().uri(
                    "/repos/{owner}/{repository}/stats/commit_activity",
                    owner,
                    repository
            ));

            if (response.statusCode().value() == 200) {
                return Optional.of(response.body());
            }
            if (response.statusCode().value() == 204 || response.statusCode().value() == 404) {
                return Optional.empty();
            }
            if (response.statusCode().value() != 202) {
                throw apiFailure(response, "commit activity for " + owner + "/" + repository);
            }
            if (attempt < statsRetries) {
                sleepBeforeRetry(attempt);
            }
        }

        return Optional.empty();
    }

    private Optional<JsonNode> optionalContent(ApiResponse response, String resource) {
        if (response.statusCode().value() == 404 || response.statusCode().value() == 409) {
            return Optional.empty();
        }
        return Optional.of(requireSuccess(response, resource));
    }

    private JsonNode requireSuccess(ApiResponse response, String resource) {
        if (!response.statusCode().is2xxSuccessful()) {
            throw apiFailure(response, resource);
        }
        return response.body();
    }

    private GithubApiException apiFailure(ApiResponse response, String resource) {
        String githubMessage = response.body().path("message").stringValue("Unknown GitHub API error");
        return new GithubApiException(
                response.statusCode().value(),
                "Could not fetch " + resource + ": " + githubMessage
        );
    }

    private ApiResponse execute(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((clientRequest, clientResponse) -> {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(clientResponse.getHeaders());
            byte[] bytes = clientResponse.getBody().readAllBytes();
            JsonNode body = bytes.length == 0
                    ? objectMapper.getNodeFactory().nullNode()
                    : objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
            checkRateLimit(headers);
            return new ApiResponse(clientResponse.getStatusCode(), headers, body);
        });
    }

    private void checkRateLimit(HttpHeaders headers) {
        String remainingValue = headers.getFirst("X-RateLimit-Remaining");
        if (!StringUtils.hasText(remainingValue)) {
            return;
        }

        try {
            int remaining = Integer.parseInt(remainingValue);
            if (remaining <= lowRateLimitThreshold) {
                String resetValue = headers.getFirst("X-RateLimit-Reset");
                String resetMessage = StringUtils.hasText(resetValue)
                        ? " Reset at " + Instant.ofEpochSecond(Long.parseLong(resetValue)) + "."
                        : "";
                throw new GithubRateLimitException(
                        "GitHub API rate limit is nearly exhausted (" + remaining + " requests remaining)." + resetMessage
                );
            }
        } catch (NumberFormatException ignored) {
            // Ignore malformed optional rate-limit headers and continue processing the response.
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(statsRetryDelay.multipliedBy(attempt));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GithubApiException(503, "Interrupted while waiting for GitHub statistics");
        }
    }

    private record ApiResponse(HttpStatusCode statusCode, HttpHeaders headers, JsonNode body) {
    }
}
