package com.webworks.alert_case_service.cases;

import com.webworks.alert_case_service.alert.Alert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "surveillance_case")
public class SurveillanceCase {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(length = 64)
    private String assignee;

    @Column(nullable = false)
    private int priority = 3;

    @Column(name = "opened_by", nullable = false, length = 64)
    private String openedBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private long version;

    @ManyToMany
    @JoinTable(name = "case_alert",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "alert_id"))
    private Set<Alert> alerts = new LinkedHashSet<>();

    protected SurveillanceCase() {
    }

    public SurveillanceCase(String title, String openedBy, Collection<Alert> alerts) {
        this.title = title;
        this.openedBy = openedBy;
        this.openedAt = Instant.now();
        this.alerts.addAll(alerts);
    }

    /** The only way to change status: enforces the workflow from Step 4.1. */
    public void moveTo(CaseStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        status = next;
        if (next == CaseStatus.CLOSED) closedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public CaseStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public int getPriority() { return priority; }
    public String getOpenedBy() { return openedBy; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public long getVersion() { return version; }
    public Set<Alert> getAlerts() { return alerts; }

    public void setAssignee(String assignee) { this.assignee = assignee; }
    public void setPriority(int priority) { this.priority = priority; }
}