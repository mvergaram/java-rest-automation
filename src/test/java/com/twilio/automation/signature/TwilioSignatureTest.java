package com.twilio.automation.signature;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic, offline regression test for the signing algorithm (HMAC-SHA1 over the URL
 * plus POST params sorted by ordinal/case-sensitive key order, then Base64-encoded).
 *
 * <p>The expected value below was NOT sourced from Twilio's live documentation — this
 * environment had no working internet access when the suite was written. It was instead
 * cross-checked by computing the same HMAC-SHA1 independently (outside this codebase) for
 * this exact input and confirming both results match. Before relying on this as a
 * "known-good against Twilio" vector, verify the expected value against Twilio's current
 * "Validating Requests" security guide.
 */
class TwilioSignatureTest {

    @Test
    void sortsParamsOrdinallyAndMatchesTheIndependentlyComputedHmac() {
        String authToken = "12345";
        String url = "https://mycompany.com/myapp.php?foo=1&bar=2";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("CallSid", "CA1234567890ABCDE");
        params.put("Caller", "+14158675310");
        params.put("Digits", "1234");
        params.put("From", "+14158675310");
        params.put("To", "+18005551212");

        String signature = TwilioSignature.compute(authToken, url, params);

        assertThat(signature).isEqualTo("GvWf1cFY/Q7PnoempGyD5oXAezc=");
    }
}
