package com.webworks.alert_case_service.cases;

import com.webworks.alert_case_service.alert.Alert;
import com.webworks.alert_case_service.alert.AlertRepository;
import com.webworks.alert_case_service.alert.AlertStatus;
import com.webworks.alert_case_service.alert.AlertSummary;
import com.webworks.alert_case_service.web.CurrentUser;
import com.webworks.alert_case_service.web.NotFoundException;
import com.webworks.alert_case_service.web.PageResponse;
import com.webworks.tradecommon.Json;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CaseService {

    private final CaseRepository cases;
    private final CaseEventRepository events;
    private final AlertRepository alerts;
    private final CurrentUser user;

    public CaseService(CaseRepository cases, CaseEventRepository events, AlertRepository alerts, CurrentUser user) {
        this.cases = cases;
        this.events = events;
        this.alerts = alerts;
        this.user = user;
    }

    public CaseDetail open(CreateCaseRequest req) {
        List<Alert> found = alerts.findAllById(req.alertIds());
        if (found.size() != new HashSet<>(req.alertIds()).size()) {
            throw new NotFoundException("One or more alerts do not exist");
        }
        found.forEach(a -> a.setStatus(AlertStatus.IN_CASE));
        SurveillanceCase c = cases.save(new SurveillanceCase(req.title(), user.name(), found));
        record(c, "CASE_OPENED", Map.of("title", req.title(), "alertIds", req.alertIds()));
        return detail(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseSummary> list(CaseStatus status, Pageable pageable) {
        var page = status == null ? cases.findAll(pageable) : cases.findByStatus(status, pageable);
        return PageResponse.of(page.map(CaseSummary::from));
    }

    @Transactional(readOnly = true)
    public CaseDetail get(UUID id) {
        return detail(find(id));
    }

    public CaseDetail update(UUID id, UpdateCaseRequest req) {
        SurveillanceCase c = find(id);

        // Step 4.3: the client must have seen the latest version.
        if (req.version() != c.getVersion()) {
            throw new ObjectOptimisticLockingFailureException(SurveillanceCase.class, id);
        }

        if (req.status() != null && req.status() != c.getStatus()) {
            if (req.status() == CaseStatus.CLOSED && !user.isSupervisor()) {
                throw new AccessDeniedException("Only a supervisor can close a case");
            }
            CaseStatus old = c.getStatus();
            c.moveTo(req.status());                       // throws InvalidTransitionException
            record(c, "STATUS_CHANGED", change(old, req.status()));
        }

        if (req.assignee() != null && !req.assignee().equals(c.getAssignee())) {
            if (!req.assignee().equals(user.name()) && !user.isSupervisor()) {
                throw new AccessDeniedException("Only a supervisor can assign a case to someone else");
            }
            String old = c.getAssignee();
            c.setAssignee(req.assignee());
            record(c, "ASSIGNED", change(old, req.assignee()));
        }

        if (req.priority() != null && req.priority() != c.getPriority()) {
            int old = c.getPriority();
            c.setPriority(req.priority());
            record(c, "PRIORITY_CHANGED", change(old, req.priority()));
        }

        cases.saveAndFlush(c);   // flush now, so a concurrent change fails inside this request
        return detail(c);
    }

    public CaseEventView comment(UUID id, String text) {
        SurveillanceCase c = find(id);
        return CaseEventView.from(record(c, "COMMENT_ADDED", Map.of("text", text)));
    }

    @Transactional(readOnly = true)
    public List<CaseEventView> events(UUID id) {
        find(id);
        return events.findByCaseIdOrderByIdAsc(id).stream().map(CaseEventView::from).toList();
    }

    // ---- helpers ----

    private SurveillanceCase find(UUID id) {
        return cases.findById(id).orElseThrow(() -> new NotFoundException("Case " + id + " not found"));
    }

    /** Step 4.4: every change writes an audit row in the SAME transaction. */
    private CaseEvent record(SurveillanceCase c, String action, Map<String, Object> detail) {
        return events.save(new CaseEvent(c.getId(), user.name(), action, Json.write(detail)));
    }

    private static Map<String, Object> change(Object from, Object to) {
        Map<String, Object> m = new LinkedHashMap<>();   // allows null, unlike Map.of
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private CaseDetail detail(SurveillanceCase c) {
        List<AlertSummary> linked = c.getAlerts().stream().map(AlertSummary::from).toList();
        List<CaseEventView> history = c.getId() == null ? List.of()
                : events.findByCaseIdOrderByIdAsc(c.getId()).stream().map(CaseEventView::from).toList();
        return new CaseDetail(CaseSummary.from(c), linked, history);
    }
}