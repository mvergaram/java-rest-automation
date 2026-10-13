package com.twilio.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Single entry point for configuration, resolved in this order: a real environment
 * variable, then a value from a {@code .env} file at the project root (see
 * {@link DotenvLoader}), then — for non-secret settings only — a default from
 * {@code config.properties}. A real environment variable always wins so CI (which injects
 * secrets as actual env vars, never as a file) and one-off overrides keep working unchanged.
 */
public final class TestConfig {

    private static final String PROPERTIES_RESOURCE = "/config.properties";
    private static final Properties DEFAULTS = loadDefaults();
    private static final Map<String, String> DOTENV = DotenvLoader.load(Path.of(".env"));

    private TestConfig() {}

    public static String presignBaseUrl() {
        return requiredEnv("PRESIGN_BASE_URL");
    }

    public static String twilioAccountSid() {
        return requiredEnv("TWILIO_ACCOUNT_SID");
    }

    public static String twilioAuthToken() {
        return requiredEnv("TWILIO_AUTH_TOKEN");
    }

    public static String twilioServiceSid() {
        return requiredEnv("TWILIO_SERVICE_SID");
    }

    public static MediaMode mediaMode() {
        String value = resolveValueOrDefault("MEDIA_MODE", DEFAULTS.getProperty("media.mode"));
        return MediaMode.valueOf(value.trim().toUpperCase());
    }

    public static boolean verboseHttpLogs() {
        return Boolean.parseBoolean(resolveValueOrDefault("VERBOSE_HTTP_LOGS", "false"));
    }

    public static Duration pollInitialDelay() {
        return Duration.ofSeconds(longProperty("poll.initial-delay-seconds"));
    }

    public static Duration pollTimeout() {
        return Duration.ofSeconds(longProperty("poll.timeout-seconds"));
    }

    public static Duration pollInterval() {
        return Duration.ofSeconds(longProperty("poll.interval-seconds"));
    }

    /** True once every required secret is present — lets the E2E suite skip cleanly otherwise. */
    public static boolean isFullyConfigured() {
        return resolveValue("PRESIGN_BASE_URL") != null
            && resolveValue("TWILIO_ACCOUNT_SID") != null
            && resolveValue("TWILIO_AUTH_TOKEN") != null
            && resolveValue("TWILIO_SERVICE_SID") != null;
    }

    private static String requiredEnv(String key) {
        String value = resolveValue(key);
        if (value == null) {
            throw new IllegalStateException(
                "Missing required configuration '" + key + "'. Copy .env.example to .env at the "
                    + "project root and fill it in, or export it as a real environment variable.");
        }
        return value;
    }

    private static String resolveValueOrDefault(String key, String fallback) {
        String value = resolveValue(key);
        return value != null ? value : fallback;
    }

    private static String resolveValue(String key) {
        return resolveValue(key, System.getenv(), DOTENV);
    }

    /** Pure and testable: takes both maps as arguments instead of reading the real environment. */
    static String resolveValue(String key, Map<String, String> env, Map<String, String> dotenv) {
        String fromEnv = env.get(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDotenv = dotenv.get(key);
        return (fromDotenv != null && !fromDotenv.isBlank()) ? fromDotenv : null;
    }

    private static long longProperty(String key) {
        return Long.parseLong(DEFAULTS.getProperty(key));
    }

    private static Properties loadDefaults() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + PROPERTIES_RESOURCE);
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load " + PROPERTIES_RESOURCE, e);
        }
        return properties;
    }
}
