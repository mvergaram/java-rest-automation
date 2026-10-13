package com.twilio.automation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileReturnsEmptyMap() {
        Path missing = tempDir.resolve("no-such-" + UUID.randomUUID() + ".env");

        assertThat(DotenvLoader.load(missing)).isEmpty();
    }

    @Test
    void parsesSimpleKeyValuePairs() throws IOException {
        Path file = writeEnvFile("FOO=bar");

        assertThat(DotenvLoader.load(file)).containsEntry("FOO", "bar");
    }

    @Test
    void skipsCommentLinesAndBlankLines() throws IOException {
        Path file = writeEnvFile(
            "# a comment",
            "",
            "FOO=bar");

        assertThat(DotenvLoader.load(file)).containsExactly(Map.entry("FOO", "bar"));
    }

    @Test
    void valueContainingEqualsOrHashIsPreservedAfterFirstEquals() throws IOException {
        Path file = writeEnvFile("PRESIGN_BASE_URL=https://x.com/a?b=1&c=2#frag");

        assertThat(DotenvLoader.load(file))
            .containsEntry("PRESIGN_BASE_URL", "https://x.com/a?b=1&c=2#frag");
    }

    @Test
    void stripsMatchingSingleOrDoubleQuotes() throws IOException {
        Path file = writeEnvFile(
            "FOO=\"bar baz\"",
            "QUX='bar baz'");

        Map<String, String> values = DotenvLoader.load(file);
        assertThat(values).containsEntry("FOO", "bar baz");
        assertThat(values).containsEntry("QUX", "bar baz");
    }

    @Test
    void malformedLineWithoutEqualsSignIsIgnored() throws IOException {
        Path file = writeEnvFile(
            "no-equals-here",
            "FOO=bar");

        assertThat(DotenvLoader.load(file)).containsExactly(Map.entry("FOO", "bar"));
    }

    @Test
    void lastOccurrenceOfRepeatedKeyWins() throws IOException {
        Path file = writeEnvFile(
            "FOO=1",
            "FOO=2");

        assertThat(DotenvLoader.load(file)).containsEntry("FOO", "2");
    }

    private Path writeEnvFile(String... lines) throws IOException {
        Path file = tempDir.resolve(".env");
        Files.write(file, java.util.List.of(lines));
        return file;
    }
}
