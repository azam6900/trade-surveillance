package com.webworks.alert_case_service.alert;

import com.webworks.tradecommon.AlertEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert")
public class Alert {

    @Id
    @GeneratedValue                     // Hibernate generates a random UUID on insert
    private UUID id;

    @Column(name = "alert_key", nullable = false, unique = true, length = 64)
    private String alertKey;

    @Column(nullable = false, length = 32)
    private String rule;

    @Column(name = "trader_id", nullable = false, length = 32)
    private String traderId;

    @Column(length = 16)
    private String symbol;

    @Column(nullable = false)
    private int severity;

    @Enumerated(EnumType.STRING)        // store "NEW", not 0
    @Column(nullable = false, length = 16)
    private AlertStatus status = AlertStatus.NEW;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Alert() {
        // required by JPA
    }

    public static Alert from(AlertEvent a) {
        Alert alert = new Alert();
        alert.alertKey = a.alertKey();
        alert.rule = a.rule();
        alert.traderId = a.traderId();
        alert.symbol = a.symbol();
        alert.severity = a.severity();
        alert.windowStart = a.windowStart();
        alert.windowEnd = a.windowEnd();
        alert.createdAt = Instant.now();
        return alert;
    }

    public UUID getId() { return id; }
    public String getAlertKey() { return alertKey; }
    public String getRule() { return rule; }
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public int getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public Instant getWindowStart() { return windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(AlertStatus status) { this.status = status; }
}