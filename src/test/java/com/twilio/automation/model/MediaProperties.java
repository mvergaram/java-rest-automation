package com.twilio.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaProperties(@JsonProperty("media_url") String mediaUrl) {}
