package com.devtwin.twinengine;

import com.devtwin.analyzer.NormalizedRepo;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record DeveloperTwin(
        String platform,
        String username,
        String displayName,
        JsonNode profile,
        String aiSummary,
        Instant builtAt,
        List<SkillClaim> skills,
        List<NormalizedRepo> topRepositories
) {
    public DeveloperTwin {
        skills = List.copyOf(skills);
        topRepositories = List.copyOf(topRepositories);
    }
}
