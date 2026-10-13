package com.twilio.automation.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers the 3-level precedence (real env var > .env > config.properties default) in isolation. */
class TestConfigTest {

    @Test
    void realEnvironmentVariableWinsOverDotenvValue() {
        Map<String, String> env = Map.of("FOO", "from-env");
        Map<String, String> dotenv = Map.of("FOO", "from-dotenv");

        assertThat(TestConfig.resolveValue("FOO", env, dotenv)).isEqualTo("from-env");
    }

    @Test
    void dotenvValueUsedWhenEnvironmentVariableAbsent() {
        Map<String, String> env = Map.of();
        Map<String, String> dotenv = Map.of("FOO", "from-dotenv");

        assertThat(TestConfig.resolveValue("FOO", env, dotenv)).isEqualTo("from-dotenv");
    }

    @Test
    void blankEnvironmentVariableFallsBackToDotenv() {
        Map<String, String> env = Map.of("FOO", "");
        Map<String, String> dotenv = Map.of("FOO", "from-dotenv");

        assertThat(TestConfig.resolveValue("FOO", env, dotenv)).isEqualTo("from-dotenv");
    }

    @Test
    void missingEverywhereReturnsNull() {
        assertThat(TestConfig.resolveValue("FOO", Map.of(), Map.of())).isNull();
    }
}
