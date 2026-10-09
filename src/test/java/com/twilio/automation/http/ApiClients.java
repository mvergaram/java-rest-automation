package com.twilio.automation.http;

import com.twilio.automation.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;

/** Reusable {@link RequestSpecification}s — one per API, since base URI and auth differ. */
public final class ApiClients {

    private static final String INTELLIGENCE_BASE_URI = "https://intelligence.twilio.com/v2";

    private ApiClients() {}

    public static RequestSpecification presign() {
        return new RequestSpecBuilder()
            .setBaseUri(TestConfig.presignBaseUrl())
            .setConfig(restAssuredConfig())
            .build();
    }

    public static RequestSpecification intelligence() {
        return new RequestSpecBuilder()
            .setBaseUri(INTELLIGENCE_BASE_URI)
            .setAuth(RestAssured.basic(TestConfig.twilioAccountSid(), TestConfig.twilioAuthToken()))
            .setConfig(restAssuredConfig())
            .build();
    }

    private static RestAssuredConfig restAssuredConfig() {
        LogConfig logConfig = TestConfig.verboseHttpLogs()
            ? LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL).enablePrettyPrinting(true)
            : LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssuredConfig.config().logConfig(logConfig);
    }
}
