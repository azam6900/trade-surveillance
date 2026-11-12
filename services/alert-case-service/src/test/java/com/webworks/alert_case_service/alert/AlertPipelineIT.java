package com.webworks.alert_case_service.alert;

import com.webworks.alert_case_service.TestcontainersConfig;
import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.Topics;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfig.class)
class AlertPipelineIT {

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired AlertRepository alerts;
    @Autowired AlertEvidenceRepository evidence;
    @Autowired MeterRegistry registry;

    @Test
    void sameAlertDeliveredTwiceIsStoredOnce() {
        String key = UUID.randomUUID().toString().replace("-", "");
        Instant start = Instant.parse("2026-10-27T14:00:00Z");
        String json = Json.write(new AlertEvent(key, "SPOOFING", "T-77", "RY.TO", 80,
                start, start.plusSeconds(10), Map.of("placedQty", 50000)));

        kafka.send(Topics.ALERTS, "T-77", json);
        kafka.send(Topics.ALERTS, "T-77", json);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(registry.counter("alerts.duplicates").count()).isGreaterThanOrEqualTo(1.0));

        Alert stored = alerts.findByAlertKey(key).orElseThrow();
        assertThat(alerts.findAll()).filteredOn(a -> a.getAlertKey().equals(key)).hasSize(1);
        assertThat(evidence.findById(stored.getId())).isPresent();
    }
}