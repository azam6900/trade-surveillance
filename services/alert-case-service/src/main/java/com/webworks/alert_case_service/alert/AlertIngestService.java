package com.webworks.alert_case_service.alert;

import com.webworks.tradecommon.AlertEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AlertIngestService {

    static final Duration DEDUPE_TTL = Duration.ofHours(1);

    private final AlertRepository alerts;
    private final AlertEvidenceRepository evidence;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher events;
    private final Counter duplicates;
    private final MeterRegistry registry;
    private final Timer detectionLatency;
    public AlertIngestService(AlertRepository alerts, AlertEvidenceRepository evidence, StringRedisTemplate redis,
                              ApplicationEventPublisher events, MeterRegistry registry) {
        this.alerts = alerts;
        this.evidence = evidence;
        this.redis = redis;
        this.events = events;
        this.duplicates = Counter.builder("alerts.duplicates")
                .description("Alert messages recognized as duplicates")
                .register(registry);
        this.registry = registry;
        this.detectionLatency = Timer.builder("alert.detection.latency")
                .description("Time from the triggering order event to the stored alert")
                .publishPercentileHistogram()
                .register(registry);
    }

    @Transactional
    public void handle(AlertEvent a) {
        String dedupeKey = "alert:dedupe:" + a.alertKey();

        // 1. Fast path: seen recently? Then stop.
        if (Boolean.TRUE.equals(redis.hasKey(dedupeKey))) {
            duplicates.increment();
            return;
        }

        // 2. The truth: the unique alert_key in PostgreSQL.
        Optional<Alert> existing = alerts.findByAlertKey(a.alertKey());
        Alert alert = existing.orElseGet(() -> alerts.save(Alert.from(a)));
        if (existing.isPresent()) duplicates.increment();

        // 3. Evidence: an upsert by alert id, safe to repeat.
        evidence.save(new AlertEvidence(alert.getId(), a.rule(), a.evidence(), Instant.now()));

        // 4. Only after the commit: remember the key, and announce a NEW alert.
        afterCommit(() -> redis.opsForValue().set(dedupeKey, "1", DEDUPE_TTL));
        if (existing.isEmpty()) {
            events.publishEvent(AlertCreated.of(alert));
            Counter.builder("alerts.created").tag("rule", a.rule()).register(registry).increment();
            Object trigger = a.evidence().get("triggerTs");
            if (trigger != null) {
                detectionLatency.record(Duration.between(Instant.parse(trigger.toString()), Instant.now()));
            }
        }
    }

    /** Runs the action after a successful commit; runs it at once when there is no transaction (unit tests). */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}