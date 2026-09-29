package com.devtwin.api;

import com.devtwin.connector.IngestStatus;
import com.devtwin.connector.RawDataStore;
import com.devtwin.connector.RawFetchResult;
import com.devtwin.connector.github.GithubConnector;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private static final String GITHUB_USERNAME_PATTERN = "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$";

    private final GithubConnector githubConnector;
    private final RawDataStore rawDataStore;

    public IngestController(GithubConnector githubConnector, RawDataStore rawDataStore) {
        this.githubConnector = githubConnector;
        this.rawDataStore = rawDataStore;
    }

    @PostMapping("/{username}")
    public RawFetchResult ingest(
            @PathVariable
            @Pattern(regexp = GITHUB_USERNAME_PATTERN, message = "Invalid GitHub username")
            String username
    ) {
        return githubConnector.fetch(username);
    }

    @GetMapping("/{username}/status")
    public IngestStatus status(
            @PathVariable
            @Pattern(regexp = GITHUB_USERNAME_PATTERN, message = "Invalid GitHub username")
            String username
    ) {
        return rawDataStore.status(githubConnector.platform(), username);
    }
}
