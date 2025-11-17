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
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EcsSignatureFilter extends OncePerRequestFilter {

    private final EcsCallbackSignatureValidator ecsCallbackSignatureValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only validate ECS callbacks under /api/videos and only for POST requests
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("[EcsSignatureFilter] Request: " + method + " " + path);
        if (!path.startsWith("/api/videos") || !"POST".equalsIgnoreCase(method)) {
            System.out.println("[EcsSignatureFilter] Bypassing signature validation (not POST to /api/videos)");
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest =
                (request instanceof ContentCachingRequestWrapper) ? (ContentCachingRequestWrapper) request
                        : new ContentCachingRequestWrapper(request);

        // Read and cache body for signature verification and downstream controllers
        String body = StreamUtils.copyToString(wrappedRequest.getInputStream(), StandardCharsets.UTF_8);
        wrappedRequest.setAttribute("cachedRequestBody", body);

        String signature = request.getHeader("X-ECS-Signature");
        String timestamp = request.getHeader("X-ECS-Timestamp");

        System.out.println("[EcsSignatureFilter] Validating signature - timestamp: " + timestamp);
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

        filterChain.doFilter(wrappedRequest, response);
    }
}
