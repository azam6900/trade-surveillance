package com.webworks.alert_case_service.alert;

import java.util.Map;

public record AlertDetail(AlertSummary alert, Map<String, Object> evidence) {
}