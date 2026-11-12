package com.webworks.alert_case_service.alert;

import com.webworks.tradecommon.AlertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class AlertSpecsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired AlertRepository alerts;

    private Alert alert(String key, String rule, String trader) {
        Instant start = Instant.parse("2026-10-27T14:00:00Z");
        return Alert.from(new AlertEvent(key, rule, trader, "RY.TO", 80, start, start.plusSeconds(10), Map.of()));
    }

    @BeforeEach
    void seed() {
        alerts.save(alert("k-a", "SPOOFING", "T-77"));
        alerts.save(alert("k-b", "CANCEL_RATIO", "T-99"));
        Alert dismissed = alert("k-c", "SPOOFING", "T-99");
        dismissed.setStatus(AlertStatus.DISMISSED);
        alerts.save(dismissed);
    }

    private long count(AlertFilter f) {
        return alerts.count(AlertSpecs.matching(f));
    }

    @Test void noFilterReturnsAll()   { assertThat(count(new AlertFilter(null, null, null, null, null))).isEqualTo(3); }
    @Test void byRule()               { assertThat(count(new AlertFilter("SPOOFING", null, null, null, null))).isEqualTo(2); }
    @Test void byStatus()             { assertThat(count(new AlertFilter(null, AlertStatus.DISMISSED, null, null, null))).isEqualTo(1); }
    @Test void byTrader()             { assertThat(count(new AlertFilter(null, null, "T-99", null, null))).isEqualTo(2); }
    @Test void byRuleAndTrader()      { assertThat(count(new AlertFilter("SPOOFING", null, "T-99", null, null))).isEqualTo(1); }

    @Test
    void byCreatedTimeRange() {
        Instant now = Instant.now();
        assertThat(count(new AlertFilter(null, null, null, now.minusSeconds(60), now.plusSeconds(60)))).isEqualTo(3);
        assertThat(count(new AlertFilter(null, null, null, now.plusSeconds(60), null))).isZero();
    }
}