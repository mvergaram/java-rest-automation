package com.twilio.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

/**
 * Single entry point for configuration. Secrets always come from environment variables;
 * {@code config.properties} only supplies non-secret defaults (timing, media mode).
 */
public final class TestConfig {

    private static final String PROPERTIES_RESOURCE = "/config.properties";
    private static final Properties DEFAULTS = loadDefaults();

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
        String value = System.getenv().getOrDefault("MEDIA_MODE", DEFAULTS.getProperty("media.mode"));
        return MediaMode.valueOf(value.trim().toUpperCase());
    }

    public static boolean verboseHttpLogs() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("VERBOSE_HTTP_LOGS", "false"));
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
        return System.getenv("PRESIGN_BASE_URL") != null
            && System.getenv("TWILIO_ACCOUNT_SID") != null
            && System.getenv("TWILIO_AUTH_TOKEN") != null
            && System.getenv("TWILIO_SERVICE_SID") != null;
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Missing required environment variable '" + key + "'. Copy .env.example to .env "
                    + "(or export it directly) before running the E2E suite.");
        }
        return value;
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
