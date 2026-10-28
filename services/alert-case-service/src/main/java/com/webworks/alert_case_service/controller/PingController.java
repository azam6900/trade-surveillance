package com.webworks.alert_case_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PingController {
    @GetMapping("/api/ping")
    Map<String, String> ping(Authentication auth) {
        return Map.of("status", "ok", "user", String.valueOf(auth.getName()));
    }
}