package com.twilio.automation.config;

/**
 * How an {@code AudioFixture.audioRef()} is turned into the {@code media_url} sent to
 * the presign endpoint. See {@code com.twilio.automation.media.MediaResolver}.
 */
public enum MediaMode {
    /** audioRef is already a publicly reachable URL; it is passed straight to presign. */
    REFERENCE_ONLY,

    /** audioRef is a local file path that must be uploaded before it can be referenced. */
    UPLOAD_THEN_REFERENCE
}
