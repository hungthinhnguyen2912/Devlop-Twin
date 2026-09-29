package com.devtwin.twinengine;

import java.time.LocalDate;
import java.util.List;

public record SkillClaim(
        String techName,
        String category,
        int repoCount,
        LocalDate firstUsed,
        LocalDate lastUsed,
        List<Evidence> evidence
) {
    public SkillClaim {
        evidence = List.copyOf(evidence);
    }
}
