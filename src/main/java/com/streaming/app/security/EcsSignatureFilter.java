package com.streaming.app.security;

import com.streaming.app.util.ResponseMessages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EcsSignatureFilter extends OncePerRequestFilter {

    private final EcsCallbackSignatureValidator ecsCallbackSignatureValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("[EcsSignatureFilter] Request: " + method + " " + path);
        
        // Only validate ECS callbacks under /api/videos and only for POST requests
        if (!path.startsWith("/api/videos") || !"POST".equalsIgnoreCase(method)) {
            System.out.println("[EcsSignatureFilter] Bypassing signature validation (not POST to /api/videos)");
            filterChain.doFilter(request, response);
            return;
        }

        // Create a wrapper that allows reading the body multiple times
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        
        // Read the body for signature validation
        String body = cachedRequest.getBody();
        
        String signature = request.getHeader("X-ECS-Signature");
        String timestamp = request.getHeader("X-ECS-Timestamp");

        System.out.println("[EcsSignatureFilter] Validating signature - timestamp: " + timestamp);
        System.out.println("[EcsSignatureFilter] Body: '" + body + "'");
        
        if (signature == null || timestamp == null) {
            System.out.println("[EcsSignatureFilter] ERROR: Missing signature headers");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(ResponseMessages.MISSING_SIGNATURE_HEADERS);
            return;
        }

        try {
            ecsCallbackSignatureValidator.verify(body, signature, timestamp);
            System.out.println("[EcsSignatureFilter] Signature validation passed");
        } catch (com.streaming.app.security.InvalidSignatureException ex) {
            System.out.println("[EcsSignatureFilter] ERROR: Invalid signature - " + ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(ResponseMessages.INVALID_SIGNATURE);
            return;
        } catch (Exception ex) {
            System.out.println("[EcsSignatureFilter] ERROR: Signature validation exception - " + ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(ResponseMessages.SIGNATURE_VALIDATION_FAILED + ex.getMessage());
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }
    
    // Inner class to cache the request body
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            // Read and cache the body
            BufferedReader reader = request.getReader();
            String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            this.cachedBody = body.getBytes(StandardCharsets.UTF_8);
        }

        public String getBody() {
            return new String(this.cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
        }
    }

    private static class CachedBodyServletInputStream extends jakarta.servlet.ServletInputStream {
        private ByteArrayInputStream byteArrayInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return byteArrayInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return byteArrayInputStream.read();
        }
    }
}