package com.twilio.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serialized to a JSON string and sent as the literal value of the {@code Channel} form
 * field in the Create Transcript request — Twilio Intelligence expects a string containing
 * JSON here, not a nested multipart object.
 */
public record ChannelPayload(@JsonProperty("media_properties") MediaProperties mediaProperties) {}
