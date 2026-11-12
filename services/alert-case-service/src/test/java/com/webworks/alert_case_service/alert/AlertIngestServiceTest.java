package com.webworks.alert_case_service.alert;

import com.webworks.tradecommon.AlertEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertIngestServiceTest {

    @Mock AlertRepository alerts;
    @Mock AlertEvidenceRepository evidence;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;
    @Mock ApplicationEventPublisher events;

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    AlertIngestService service;

    final AlertEvent event = new AlertEvent("k-1", "SPOOFING", "T-77", "RY.TO", 80,
            Instant.parse("2026-10-27T14:00:00Z"), Instant.parse("2026-10-27T14:00:10Z"), Map.of("placedQty", 50000));

    @BeforeEach
    void setUp() {
        service = new AlertIngestService(alerts, evidence, redis, events, registry);
    }

    private double duplicates() {
        return registry.counter("alerts.duplicates").count();
    }

    @Test
    void redisKeyPresentMeansNothingIsSaved() {
        when(redis.hasKey("alert:dedupe:k-1")).thenReturn(true);

        service.handle(event);

        verifyNoInteractions(alerts, evidence, events);
        assertThat(duplicates()).isEqualTo(1.0);
    }

    @Test
    void newAlertIsSavedOnceAndAnnounced() {
        when(redis.hasKey("alert:dedupe:k-1")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(ops);
        when(alerts.findByAlertKey("k-1")).thenReturn(Optional.empty());
        when(alerts.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handle(event);

        verify(alerts).save(any(Alert.class));
        verify(evidence).save(any(AlertEvidence.class));
        verify(ops).set(eq("alert:dedupe:k-1"), eq("1"), any(Duration.class));
        verify(events).publishEvent(any(AlertCreated.class));
        assertThat(duplicates()).isZero();
    }

    @Test
    void existingAlertGetsEvidenceButNoSecondRow() {
        when(redis.hasKey("alert:dedupe:k-1")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(ops);
        when(alerts.findByAlertKey("k-1")).thenReturn(Optional.of(Alert.from(event)));

        service.handle(event);

        verify(alerts, never()).save(any(Alert.class));
        verify(evidence).save(any(AlertEvidence.class));
        verifyNoInteractions(events);
        assertThat(duplicates()).isEqualTo(1.0);
    }
}