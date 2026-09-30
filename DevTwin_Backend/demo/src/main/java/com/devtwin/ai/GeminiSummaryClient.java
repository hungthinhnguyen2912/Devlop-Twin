package com.devtwin.ai;

import com.devtwin.twinengine.DeveloperTwin;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
public class GeminiSummaryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiSummaryClient.class);

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public GeminiSummaryClient(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.model:gemini-flash-latest}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    public Optional<String> generate(DeveloperTwin twin) {
        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.info("Gemini summary skipped because no API key is configured");
            return Optional.empty();
        }

        try {
            String twinJson = objectMapper.writeValueAsString(new SummaryInput(
                    twin.profile(),
                    twin.skills(),
                    twin.topRepositories()
            ));
            String prompt = """
                    You are writing an evidence-based developer profile from a normalized Developer Twin.
                    Use only the supplied JSON. Never infer skills, seniority, employment, or experience that
                    are not supported by its repositories and evidence.

                    Return exactly two sections in Markdown using this format:
                    ## Tiếng Việt
                    3-5 concise Vietnamese sentences.

                    ## English
                    3-5 concise English sentences conveying the same facts naturally.

                    Mention the strongest technologies, breadth of repositories, and an observable focus.
                    Do not mention that you are an AI and do not add a title outside the two required headings.

                    Developer Twin JSON:
                    """ + twinJson;

            Client client = Client.builder().apiKey(apiKey).build();
            GenerateContentResponse response = client.models.generateContent(model, prompt, null);
            String summary = response.text();
            if (summary == null || summary.isBlank()) {
                LOGGER.warn("Gemini returned an empty developer summary");
                return Optional.empty();
            }
            return Optional.of(summary.trim());
        } catch (Exception exception) {
            LOGGER.warn("Gemini summary generation failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private record SummaryInput(
            Object profile,
            Object skills,
            Object topRepositories
    ) {
    }
}
