package com.streaming.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        System.out.println("[HealthController] GET /health - Health check requested");
        return ResponseEntity.ok("OK");
    }
}
