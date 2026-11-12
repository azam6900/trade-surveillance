package com.webworks.alert_case_service.alert;

import java.time.Instant;
import java.util.UUID;

public record AlertSummary(UUID id, String rule, String traderId, String symbol, int severity,
                           AlertStatus status, Instant windowStart, Instant windowEnd, Instant createdAt) {

    public static AlertSummary from(Alert a) {
        return new AlertSummary(a.getId(), a.getRule(), a.getTraderId(), a.getSymbol(), a.getSeverity(),
                a.getStatus(), a.getWindowStart(), a.getWindowEnd(), a.getCreatedAt());
    }
}