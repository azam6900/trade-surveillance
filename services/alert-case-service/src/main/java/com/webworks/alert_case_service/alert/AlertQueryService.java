package com.webworks.alert_case_service.alert;

import com.webworks.alert_case_service.web.NotFoundException;
import com.webworks.alert_case_service.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AlertQueryService {

    private final AlertRepository alerts;
    private final AlertEvidenceRepository evidence;

    public AlertQueryService(AlertRepository alerts, AlertEvidenceRepository evidence) {
        this.alerts = alerts;
        this.evidence = evidence;
    }

    public PageResponse<AlertSummary> search(AlertFilter filter, Pageable pageable) {
        return PageResponse.of(alerts.findAll(AlertSpecs.matching(filter), pageable).map(AlertSummary::from));
    }

    public AlertDetail get(UUID id) {
        Alert alert = alerts.findById(id).orElseThrow(() -> new NotFoundException("Alert " + id + " not found"));
        Map<String, Object> ev = evidence.findById(id).map(AlertEvidence::evidence).orElse(Map.of());
        return new AlertDetail(AlertSummary.from(alert), ev);
    }
}