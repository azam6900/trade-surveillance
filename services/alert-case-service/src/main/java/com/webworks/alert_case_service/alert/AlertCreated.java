package com.webworks.alert_case_service.alert;

import java.time.Instant;
import java.util.UUID;

public record AlertCreated(UUID alertId, String rule, String traderId, String symbol, int severity, Instant createdAt) {

    static AlertCreated of(Alert a) {
        return new AlertCreated(a.getId(), a.getRule(), a.getTraderId(), a.getSymbol(), a.getSeverity(), a.getCreatedAt());
    }
}