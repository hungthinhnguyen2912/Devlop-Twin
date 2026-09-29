package com.devtwin.api;

import com.devtwin.analyzer.AnalyzeResult;
import com.devtwin.analyzer.RepositoryAnalyzer;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/analyze")
public class AnalyzeController {

    private static final String GITHUB_USERNAME_PATTERN = "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$";

    private final RepositoryAnalyzer repositoryAnalyzer;

    public AnalyzeController(RepositoryAnalyzer repositoryAnalyzer) {
        this.repositoryAnalyzer = repositoryAnalyzer;
    }

    @PostMapping("/{username}")
    public AnalyzeResult analyze(
            @PathVariable
            @Pattern(regexp = GITHUB_USERNAME_PATTERN, message = "Invalid GitHub username")
            String username
    ) {
        return repositoryAnalyzer.analyze(username);
    }
}
