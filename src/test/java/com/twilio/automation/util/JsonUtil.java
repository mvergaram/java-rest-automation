package com.twilio.automation.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    // Twilio Intelligence operator payloads vary by operator type; unknown fields are
    // ignored rather than failing deserialization, since the suite only asserts on the
    // fields it explicitly models in com.twilio.automation.model.
    public static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {}

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize to JSON: " + value, e);
        }
    }
}
