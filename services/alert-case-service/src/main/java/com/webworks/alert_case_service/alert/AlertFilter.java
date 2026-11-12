package com.webworks.alert_case_service.alert;

import java.time.Instant;

public record AlertFilter(String rule, AlertStatus status, String traderId, Instant from, Instant to) {
}