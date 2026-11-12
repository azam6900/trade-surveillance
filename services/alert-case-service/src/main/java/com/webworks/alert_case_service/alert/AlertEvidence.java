package com.webworks.alert_case_service.alert;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document("alert_evidence")
public record AlertEvidence(@Id UUID alertId, String rule, Map<String, Object> evidence, Instant savedAt) {
}