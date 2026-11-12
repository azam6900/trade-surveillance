package com.webworks.alert_case_service.cases;

import java.time.Instant;
import java.util.Map;

public record CaseEventView(long id, String actor, String action, Map<?, ?> detail, Instant at) {

    public static CaseEventView from(CaseEvent e) {
        Map<?, ?> detail = e.getDetail() == null ? Map.of() : com.webworks.tradecommon.Json.read(e.getDetail(), Map.class);
        return new CaseEventView(e.getId(), e.getActor(), e.getAction(), detail, e.getAt());
    }
}