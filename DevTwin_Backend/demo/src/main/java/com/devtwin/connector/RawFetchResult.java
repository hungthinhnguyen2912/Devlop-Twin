package com.devtwin.connector;

import java.time.Instant;
import java.util.List;

public record RawFetchResult(
        String platform,
        String username,
        boolean cached,
        int repositories,
        int languages,
        int readmes,
        int dependencyFiles,
        int commitActivities,
        List<String> warnings,
        Instant completedAt
) {
    public RawFetchResult {
        warnings = List.copyOf(warnings);
    }
}
