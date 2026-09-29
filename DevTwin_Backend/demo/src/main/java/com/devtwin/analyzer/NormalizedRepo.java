package com.devtwin.analyzer;

import java.time.Instant;
import java.util.List;

public record NormalizedRepo(
        String name,
        String fullName,
        String description,
        String primaryLanguage,
        boolean fork,
        int stars,
        Instant createdAt,
        Instant pushedAt,
        List<DetectedTech> technologies
) {
    public NormalizedRepo {
        technologies = List.copyOf(technologies);
    }
}
