package com.streaming.app.util;

public final class ResponseMessages {
    private ResponseMessages() {}

    public static final String MISSING_SIGNATURE_HEADERS = "Missing ECS signature headers.";
    public static final String UNABLE_TO_READ_BODY = "Unable to read request body for signature verification.";
    public static final String INVALID_SIGNATURE = "Invalid ECS Signature";
    public static final String SIGNATURE_VALIDATION_FAILED = "Signature validation failed: ";
    public static final String VIDEO_ID_MISMATCH = "Video ID mismatch between URL and payload.";
    public static final String VIDEO_PROCESSED_FORMAT = "Video %d processed successfully.";
    public static final String VIDEO_FAILED_FORMAT = "Video %d marked as FAILED.";
}
