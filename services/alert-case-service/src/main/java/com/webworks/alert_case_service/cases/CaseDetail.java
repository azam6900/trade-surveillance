package com.webworks.alert_case_service.cases;

import com.webworks.alert_case_service.alert.AlertSummary;
import java.util.List;

public record CaseDetail(CaseSummary summary, List<AlertSummary> alerts, List<CaseEventView> events) {
}