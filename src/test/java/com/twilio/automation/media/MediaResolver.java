package com.twilio.automation.media;

import com.twilio.automation.config.TestConfig;
import com.twilio.automation.http.ApiClients;
import com.twilio.automation.model.PresignResponse;
import com.twilio.automation.signature.TwilioSignature;
import com.twilio.automation.support.AudioFixture;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Resolves an {@link AudioFixture#audioRef()} into the {@code media_url} needed for the
 * Create Transcript step, via the presign endpoint. The branch is controlled by
 * {@link TestConfig#mediaMode()} since it isn't fully specified whether audio fixtures are
 * already hosted or need uploading first (see AGENTS.md assumptions).
 */
public final class MediaResolver {

    private static final String PRESIGN_PATH = "/media/presign";

    private MediaResolver() {}

    public static String resolveMediaUrl(AudioFixture fixture) {
        return switch (TestConfig.mediaMode()) {
            case REFERENCE_ONLY -> presignReference(fixture.audioRef());
            case UPLOAD_THEN_REFERENCE -> throw new UnsupportedOperationException(
                "UPLOAD_THEN_REFERENCE is not implemented: the presign upload contract "
                    + "(field names, content type) must be confirmed before adding it.");
        };
    }

    private static String presignReference(String audioRef) {
        String fullUrl = TestConfig.presignBaseUrl() + PRESIGN_PATH;
        Map<String, String> params = Map.of("media_url", audioRef);
        String signature = TwilioSignature.compute(TestConfig.twilioAuthToken(), fullUrl, params);

        PresignResponse response = given()
                .spec(ApiClients.presign())
                .header("X-Twilio-Signature", signature)
                .formParams(params)
            .when()
                .post(PRESIGN_PATH)
            .then()
                .statusCode(200)
                .extract().as(PresignResponse.class);

        return response.url();
    }
}
