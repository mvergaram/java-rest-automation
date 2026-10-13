package com.twilio.automation.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, dependency-free parser for a {@code .env} file: one {@code KEY=value} assignment
 * per line, {@code #} comment lines, blank lines ignored, and matching single/double quotes
 * wrapping an entire value are stripped. Deliberately not a general dotenv implementation:
 * no variable interpolation ({@code ${FOO}}), no multi-line values, no {@code export } prefix,
 * and no inline comment stripping after a value (a value may legitimately contain {@code #},
 * e.g. a URL fragment). See {@code .env.example} for the supported subset.
 */
final class DotenvLoader {

    private DotenvLoader() {}

    /** Returns an empty map if {@code path} doesn't exist — the normal case (e.g. CI, or no local .env yet). */
    static Map<String, String> load(Path path) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + path, e);
        }

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue; // malformed line (no '=', or empty key) — skip rather than fail the whole file
            }
            String key = line.substring(0, eq).strip();
            String value = unquote(line.substring(eq + 1).strip());
            values.put(key, value); // a repeated key: the last occurrence in the file wins
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
