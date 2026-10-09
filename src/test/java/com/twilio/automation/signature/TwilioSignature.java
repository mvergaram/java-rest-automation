package com.twilio.automation.signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes the {@code X-Twilio-Signature} header value: HMAC-SHA1 (Base64-encoded) over the
 * full request URL followed by each POST parameter's key and value, concatenated in
 * alphabetical key order with no separators. This is the same algorithm Twilio's own
 * {@code RequestValidator} uses to validate inbound webhooks; here it signs an outbound call
 * to an in-house presign endpoint that follows the same convention (see AGENTS.md for the
 * assumption this relies on).
 */
public final class TwilioSignature {

    private TwilioSignature() {}

    public static String compute(String authToken, String fullUrl, Map<String, String> postParams) {
        String data = fullUrl + postParams.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + entry.getValue())
            .collect(Collectors.joining());

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute Twilio signature", e);
        }
    }
}
