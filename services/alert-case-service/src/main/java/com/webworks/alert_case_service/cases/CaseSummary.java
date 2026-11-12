package com.webworks.alert_case_service.cases;

import java.time.Instant;
import java.util.UUID;

public record CaseSummary(UUID id, String title, CaseStatus status, String assignee, int priority,
                          String openedBy, Instant openedAt, Instant closedAt, long version, int alertCount) {

    public static CaseSummary from(SurveillanceCase c) {
        return new CaseSummary(c.getId(), c.getTitle(), c.getStatus(), c.getAssignee(), c.getPriority(),
                c.getOpenedBy(), c.getOpenedAt(), c.getClosedAt(), c.getVersion(), c.getAlerts().size());
    }
}