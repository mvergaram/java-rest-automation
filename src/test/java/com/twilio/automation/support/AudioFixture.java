package com.twilio.automation.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.twilio.automation.util.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** One row of {@code src/test/resources/testdata/audio-fixtures.json}. */
public record AudioFixture(String id, String audioRef, List<String> expectedOperators) {

    private static final String FIXTURES_RESOURCE = "/testdata/audio-fixtures.json";

    public static List<AudioFixture> loadAll() {
        try (InputStream in = AudioFixture.class.getResourceAsStream(FIXTURES_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + FIXTURES_RESOURCE);
            }
            return JsonUtil.MAPPER.readValue(in, new TypeReference<List<AudioFixture>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load audio fixtures from " + FIXTURES_RESOURCE, e);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
