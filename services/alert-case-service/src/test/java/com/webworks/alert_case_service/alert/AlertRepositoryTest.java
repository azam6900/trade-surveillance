package com.webworks.alert_case_service.alert;

import com.webworks.tradecommon.AlertEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
class AlertRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    AlertRepository alerts;

    static AlertEvent event(String key, String rule, String trader) {
        Instant start = Instant.parse("2026-10-27T14:00:00Z");
        return new AlertEvent(key, rule, trader, "RY.TO", 80, start, start.plusSeconds(10), Map.of());
    }

    @Test
    void savesAndReadsAnAlert() {
        Alert saved = alerts.saveAndFlush(Alert.from(event("k-1", "SPOOFING", "T-77")));

        assertThat(saved.getId()).isNotNull();
        assertThat(alerts.findByAlertKey("k-1")).get()
                .extracting(Alert::getTraderId, Alert::getStatus)
                .containsExactly("T-77", AlertStatus.NEW);
    }

    @Test
    void sameAlertKeyTwiceIsRejectedByTheDatabase() {
        alerts.saveAndFlush(Alert.from(event("k-2", "SPOOFING", "T-77")));

        assertThatThrownBy(() -> alerts.saveAndFlush(Alert.from(event("k-2", "SPOOFING", "T-77"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}