package com.twilio.automation.e2e;

import com.twilio.automation.config.TestConfig;
import com.twilio.automation.http.ApiClients;
import com.twilio.automation.media.MediaResolver;
import com.twilio.automation.model.ChannelPayload;
import com.twilio.automation.model.MediaProperties;
import com.twilio.automation.model.OperatorResultEnvelope;
import com.twilio.automation.model.OperatorResultsPage;
import com.twilio.automation.model.TranscriptResponse;
import com.twilio.automation.support.AudioFixture;
import com.twilio.automation.util.BeanValidation;
import com.twilio.automation.util.JsonUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end: presign media -> create transcript -> poll until completed -> validate
 * OperatorResults, run once per {@link AudioFixture}.
 */
@DisplayName("Twilio Intelligence transcript pipeline")
class TranscriptE2ETest {

    @BeforeAll
    static void requireConfiguration() {
        Assumptions.assumeTrue(TestConfig.isFullyConfigured(),
            "Skipping Twilio Transcript E2E suite: required environment variables are not set "
                + "(copy .env.example to .env — see AGENTS.md).");
    }

    static Stream<Arguments> audioFixtures() {
        return AudioFixture.loadAll().stream()
            .map(fixture -> Arguments.of(Named.of(fixture.id(), fixture)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("audioFixtures")
    @DisplayName("presign -> create transcript -> poll -> validate operator results")
    void transcriptPipelineCompletesForAudioFixture(AudioFixture fixture) {
        // Step 1: presigned media URL
        String mediaUrl = MediaResolver.resolveMediaUrl(fixture);
        assertThat(mediaUrl).isNotBlank();

        // Step 2: create transcript
        String channelPayload = JsonUtil.toJson(new ChannelPayload(new MediaProperties(mediaUrl)));
        TranscriptResponse created = given()
                .spec(ApiClients.intelligence())
                .formParam("Channel", channelPayload)
                .formParam("ServiceSid", TestConfig.twilioServiceSid())
            .when()
                .post("/Transcripts")
            .then()
                .statusCode(201)
                .extract().as(TranscriptResponse.class);

        assertThat(created.sid()).isNotBlank();
        assertThat(created.status()).isEqualTo("queued");

        // Step 3: wait, then poll until completed
        await()
            .pollDelay(TestConfig.pollInitialDelay())
            .timeout(TestConfig.pollTimeout())
            .pollInterval(TestConfig.pollInterval())
            .untilAsserted(() -> {
                TranscriptResponse polled = given()
                        .spec(ApiClients.intelligence())
                    .when()
                        .get("/Transcripts/{sid}", created.sid())
                    .then()
                        .statusCode(200)
                        .extract().as(TranscriptResponse.class);

                assertThat(polled.status()).isEqualTo("completed");
            });

        // Step 4: validate OperatorResults — schema, strongly-typed model, operators, semantics
        Response response = given()
                .spec(ApiClients.intelligence())
            .when()
                .get("/Transcripts/{sid}/OperatorResults", created.sid())
            .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/operator-results-schema.json"))
                .extract().response();

        OperatorResultsPage page = response.as(OperatorResultsPage.class);
        BeanValidation.assertValid(page);

        List<String> actualOperatorNames = page.operatorResults().stream()
            .map(OperatorResultEnvelope::name)
            .toList();
        assertThat(actualOperatorNames).containsAll(fixture.expectedOperators());

        assertThat(page.operatorResults())
            .as("every operator result should report a usable confidence score")
            .allSatisfy(result -> assertThat(result.matchProbability()).isBetween(0.0, 1.0));
    }
}
