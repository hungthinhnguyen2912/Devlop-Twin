package com.devtwin.twinengine;

import com.devtwin.ai.GeminiSummaryClient;
import com.devtwin.analyzer.AnalyzeResult;
import com.devtwin.analyzer.RepositoryAnalyzer;
import com.devtwin.connector.github.GithubConnector;
import org.springframework.stereotype.Service;

@Service
public class TwinPipelineService {

    private final GithubConnector githubConnector;
    private final RepositoryAnalyzer repositoryAnalyzer;
    private final TwinService twinService;
    private final GeminiSummaryClient geminiSummaryClient;

    public TwinPipelineService(
            GithubConnector githubConnector,
            RepositoryAnalyzer repositoryAnalyzer,
            TwinService twinService,
            GeminiSummaryClient geminiSummaryClient
    ) {
        this.githubConnector = githubConnector;
        this.repositoryAnalyzer = repositoryAnalyzer;
        this.twinService = twinService;
        this.geminiSummaryClient = geminiSummaryClient;
    }

    public DeveloperTwin build(String username) {
        githubConnector.fetch(username);
        AnalyzeResult analysis = repositoryAnalyzer.analyze(username);
        DeveloperTwin twin = twinService.build(analysis.username(), analysis.repositories());
        if (twin.aiSummary() != null && !twin.aiSummary().isBlank()) {
            return twin;
        }
        return geminiSummaryClient.generate(twin)
                .map(summary -> twinService.updateAiSummary(analysis.username(), summary))
                .orElse(twin);
    }
}
