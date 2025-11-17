package com.streaming.app.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EcsCallbackSignatureValidator {

    @Value("${ecs.callback.secret}")
    private String secret;

    @Value("${ecs.callback.maxSkewMs:300000}") // 5 min
    private long allowedSkewMs;

    /**
     * Verify ECS callback signature.
     */
    public void verify(String rawBody, String providedSignature, String timestampHeader) {

        System.out.println("[EcsCallbackSignatureValidator] verify - timestamp: " + timestampHeader);
        if (timestampHeader == null || providedSignature == null) {
            System.out.println("[EcsCallbackSignatureValidator] ERROR: Missing signature headers");
            throw new InvalidSignatureException("Missing ECS signature headers");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException e) {
            throw new InvalidSignatureException("Invalid timestamp");
        }

        long now = Instant.now().toEpochMilli();
        if (Math.abs(now - timestamp) > allowedSkewMs) {
            System.out.println("[EcsCallbackSignatureValidator] ERROR: Timestamp out of range - now: " + now + ", timestamp: " + timestamp);
            throw new InvalidSignatureException("Timestamp too old or too new");
        }

        String expectedSig = generateSignature(timestampHeader, rawBody);

        if (!constantTimeEquals(expectedSig, providedSignature)) {
            System.out.println("[EcsCallbackSignatureValidator] ERROR: Signature mismatch");
            throw new InvalidSignatureException("Invalid ECS signature");
        }
        System.out.println("[EcsCallbackSignatureValidator] Signature verified successfully");
    }

    private String generateSignature(String timestamp, String body) {
        try {
            String data = timestamp + "." + body;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to compute ECS signature", e);
        }
    }

    /** Prevent timing attacks */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
