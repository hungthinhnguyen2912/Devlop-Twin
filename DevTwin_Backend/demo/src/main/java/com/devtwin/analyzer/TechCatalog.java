package com.devtwin.analyzer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class TechCatalog {

    private final Map<String, Definition> languages = new LinkedHashMap<>();
    private final List<Rule> dependencies = new ArrayList<>();
    private final List<Rule> dockerImages = new ArrayList<>();

    public TechCatalog(ObjectMapper objectMapper) throws IOException {
        ClassPathResource resource = new ClassPathResource("tech-catalog.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode catalog = objectMapper.readTree(inputStream);
            loadLanguages(catalog.path("languages"));
            loadRules(catalog.path("dependencies"), dependencies);
            loadRules(catalog.path("dockerImages"), dockerImages);
        }
    }

    public Optional<Definition> findLanguage(String githubLanguage) {
        return Optional.ofNullable(languages.get(normalize(githubLanguage)));
    }

    public List<Definition> findDependencies(String identifier) {
        return findMatches(dependencies, identifier);
    }

    public List<Definition> findDockerImages(String image) {
        return findMatches(dockerImages, image);
    }

    private void loadLanguages(JsonNode node) {
        node.forEachEntry((language, definition) -> languages.put(
                normalize(language),
                definition(definition)
        ));
    }

    private void loadRules(JsonNode node, List<Rule> target) {
        node.forEach(rule -> target.add(new Rule(
                normalize(rule.path("contains").stringValue("")),
                definition(rule)
        )));
    }

    private Definition definition(JsonNode node) {
        return new Definition(
                node.path("name").stringValue("unknown"),
                node.path("category").stringValue("tool")
        );
    }

    private List<Definition> findMatches(List<Rule> rules, String value) {
        String normalizedValue = normalize(value);
        return rules.stream()
                .filter(rule -> !rule.contains().isBlank() && normalizedValue.contains(rule.contains()))
                .map(Rule::definition)
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    public record Definition(String name, String category) {
    }

    private record Rule(String contains, Definition definition) {
    }
}
