package com.webworks.alert_case_service.cases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_event")
public class CaseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // BIGSERIAL: the database numbers the rows
    private Long id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(columnDefinition = "text")
    private String detail;          // JSON text, e.g. {"from":"OPEN","to":"IN_REVIEW"}

    @Column(nullable = false)
    private Instant at;

    protected CaseEvent() {
    }

    public CaseEvent(UUID caseId, String actor, String action, String detail) {
        this.caseId = caseId;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
        this.at = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getCaseId() { return caseId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public Instant getAt() { return at; }
}