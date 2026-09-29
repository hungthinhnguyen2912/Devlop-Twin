package com.devtwin.analyzer;

import java.time.Instant;
import java.util.List;

public record AnalyzeResult(
        String platform,
        String username,
        int rawRepositories,
        int analyzedRepositories,
        int skippedForks,
        int detectedTechnologies,
        Instant analyzedAt,
        List<NormalizedRepo> repositories
) {
    public AnalyzeResult {
        repositories = List.copyOf(repositories);
    }
}
