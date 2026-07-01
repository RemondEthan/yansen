package com.glodon.mordor.yansen.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: Loads {@link YansenSettings} from YAML. Loading priority:
 *   1. classpath yansen.yml (defaults)
 *   2. external file at $YANSEN_CONFIG_FILE (overrides, field-wise merged on top of defaults)
 * ${ENV_VAR:default} placeholders are resolved before binding to records.
 */
public final class YansenConfig {

    private static final String CONFIG_FILE_CLASSPATH = "/yansen.yml";
    private static final String ENV_CONFIG_FILE = "YANSEN_CONFIG_FILE";

    private YansenConfig() {}

    public static YansenSettings load() {
        YansenSettings defaults = loadFromClasspath();
        String externalPath = System.getenv(ENV_CONFIG_FILE);
        if (externalPath != null && !externalPath.isBlank()) {
            return defaults.mergedWith(loadFromFile(Path.of(externalPath)));
        }
        return defaults;
    }

    static YansenSettings loadFromClasspath() {
        try (InputStream is = YansenConfig.class.getResourceAsStream(CONFIG_FILE_CLASSPATH)) {
            if (is == null) {
                return new YansenSettings(null, null);
            }
            return parse(is);
        } catch (IOException e) {
            throw new YansenConfigException("Failed to read classpath yansen.yml", e);
        }
    }

    static YansenSettings loadFromFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        } catch (IOException e) {
            throw new YansenConfigException("Failed to read config file: " + path, e);
        }
    }

    static YansenSettings parse(InputStream is) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            JsonNode root = mapper.readTree(is);
            JsonNode resolved = resolvePlaceholders(root);
            return mapper.treeToValue(resolved, YansenSettings.class);
        } catch (IOException e) {
            throw new YansenConfigException("Failed to parse YAML configuration", e);
        }
    }

    private static JsonNode resolvePlaceholders(JsonNode node) {
        if (node.isTextual()) {
            return new TextNode(PlaceholderResolver.resolve(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            node.fields().forEachRemaining(e -> out.set(e.getKey(), resolvePlaceholders(e.getValue())));
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = JsonNodeFactory.instance.arrayNode();
            node.forEach(child -> out.add(resolvePlaceholders(child)));
            return out;
        }
        return node;
    }
}
