package com.devtwin.analyzer;

import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TechDetector {

    private static final Pattern GRADLE_COORDINATE = Pattern.compile("['\"]([^'\"]+:[^'\"]+)['\"]");
    private static final Pattern DOCKER_FROM = Pattern.compile(
            "(?im)^\\s*FROM\\s+(?:--platform=\\S+\\s+)?([^\\s]+)"
    );
    private static final Pattern COMPOSE_IMAGE = Pattern.compile(
            "(?im)^\\s*image\\s*:\\s*['\"]?([^\\s'\"#]+)"
    );

    private final TechCatalog catalog;
    private final ObjectMapper objectMapper;

    public TechDetector(TechCatalog catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    public List<DetectedTech> detect(JsonNode languages, List<DependencyFile> dependencyFiles) {
        DetectionAccumulator detected = new DetectionAccumulator();
        detectLanguages(languages, detected);
        dependencyFiles.forEach(file -> detectFile(file, detected));
        return detected.values();
    }

    private void detectLanguages(JsonNode languages, DetectionAccumulator detected) {
        if (languages == null || !languages.isObject()) {
            return;
        }

        languages.forEachEntry((language, bytes) -> catalog.findLanguage(language)
                .ifPresent(definition -> detected.add(
                        definition,
                        "languages_api",
                        language + " (" + bytes.longValue(0) + " bytes)"
                )));
    }

    private void detectFile(DependencyFile file, DetectionAccumulator detected) {
        String path = file.path().toLowerCase(Locale.ROOT);
        if (path.endsWith("pom.xml")) {
            detectMaven(file.content(), detected);
        } else if (path.endsWith("build.gradle") || path.endsWith("build.gradle.kts")) {
            detectGradle(file.content(), detected);
        } else if (path.endsWith("package.json")) {
            detectNpm(file.content(), detected);
        } else if (path.endsWith("requirements.txt")) {
            detectPip(file.content(), detected);
        } else if (path.endsWith("go.mod")) {
            detectGoModules(file.content(), detected);
        } else if (path.endsWith("dockerfile")) {
            detectDockerfile(file.content(), detected);
        } else if (isComposeFile(path)) {
            detectDockerCompose(file.content(), detected);
        }
    }

    private void detectMaven(String content, DetectionAccumulator detected) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(content)));
            NodeList dependencies = document.getElementsByTagName("dependency");
            for (int index = 0; index < dependencies.getLength(); index++) {
                Element dependency = (Element) dependencies.item(index);
                String groupId = childText(dependency, "groupId");
                String artifactId = childText(dependency, "artifactId");
                detectDependency(groupId + ":" + artifactId, "maven", detected);
            }

            NodeList parents = document.getElementsByTagName("parent");
            for (int index = 0; index < parents.getLength(); index++) {
                Element parent = (Element) parents.item(index);
                detectDependency(
                        childText(parent, "groupId") + ":" + childText(parent, "artifactId"),
                        "maven",
                        detected
                );
            }
        } catch (Exception ignored) {
            detectDependency(content, "maven", detected);
        }
    }

    private void detectGradle(String content, DetectionAccumulator detected) {
        Matcher matcher = GRADLE_COORDINATE.matcher(content);
        while (matcher.find()) {
            detectDependency(matcher.group(1), "gradle", detected);
        }
    }

    private void detectNpm(String content, DetectionAccumulator detected) {
        try {
            JsonNode packageJson = objectMapper.readTree(content);
            for (String section : List.of("dependencies", "devDependencies", "peerDependencies")) {
                JsonNode dependencies = packageJson.path(section);
                dependencies.forEachEntry((name, version) -> detectDependency(name, "npm", detected));
            }
        } catch (Exception ignored) {
            detectDependency(content, "npm", detected);
        }
    }

    private void detectPip(String content, DetectionAccumulator detected) {
        content.lines()
                .map(line -> line.split("#", 2)[0].trim())
                .filter(line -> !line.isBlank() && !line.startsWith("-"))
                .map(line -> line.split("[<>=!~\\[]", 2)[0].trim())
                .forEach(dependency -> detectDependency(dependency, "pip", detected));
    }

    private void detectGoModules(String content, DetectionAccumulator detected) {
        content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.equals("require (") && !line.equals(")"))
                .filter(line -> !line.startsWith("module ") && !line.startsWith("go "))
                .map(line -> line.startsWith("require ") ? line.substring("require ".length()).trim() : line)
                .map(line -> line.split("\\s+", 2)[0])
                .forEach(dependency -> detectDependency(dependency, "go_mod", detected));
    }

    private void detectDockerfile(String content, DetectionAccumulator detected) {
        Matcher matcher = DOCKER_FROM.matcher(content);
        while (matcher.find()) {
            detectDockerImage(matcher.group(1), "dockerfile", detected);
        }
    }

    private void detectDockerCompose(String content, DetectionAccumulator detected) {
        Matcher matcher = COMPOSE_IMAGE.matcher(content);
        while (matcher.find()) {
            detectDockerImage(matcher.group(1), "docker_compose", detected);
        }
    }

    private void detectDependency(String identifier, String source, DetectionAccumulator detected) {
        catalog.findDependencies(identifier).forEach(definition -> detected.add(
                definition,
                source,
                identifier
        ));
    }

    private void detectDockerImage(String image, String source, DetectionAccumulator detected) {
        catalog.findDockerImages(image).forEach(definition -> detected.add(
                definition,
                source,
                "image: " + image
        ));
    }

    private String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        return children.getLength() == 0 ? "" : children.item(0).getTextContent().trim();
    }

    private boolean isComposeFile(String path) {
        return path.endsWith("docker-compose.yml")
                || path.endsWith("docker-compose.yaml")
                || path.endsWith("compose.yml")
                || path.endsWith("compose.yaml");
    }

    private static final class DetectionAccumulator {

        private final Map<String, DetectedTech> detected = new LinkedHashMap<>();

        void add(TechCatalog.Definition definition, String source, String detail) {
            String key = definition.name().toLowerCase(Locale.ROOT) + "|" + source;
            detected.putIfAbsent(key, new DetectedTech(
                    definition.name(),
                    definition.category(),
                    source,
                    detail
            ));
        }

        List<DetectedTech> values() {
            return new ArrayList<>(detected.values());
        }
    }
}
