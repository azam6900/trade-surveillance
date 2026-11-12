package com.webworks.alert_case_service.alert;

import com.webworks.alert_case_service.TestcontainersConfig;
import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.Topics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfig.class)
class DeadLetterIT {

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired KafkaAdmin admin;
    @Autowired AlertRepository alerts;

    @Test
    void garbageGoesToTheDltAndTheNextAlertIsStillProcessed() {
        kafka.send(Topics.ALERTS, "T-1", "not json");

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, admin.getConfigurationProperties().get("bootstrap.servers"),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(Topics.ALERTS_DLT));
            List<ConsumerRecord<String, String>> parked = new ArrayList<>();
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(parked::add);
                assertThat(parked).extracting(ConsumerRecord::value).contains("not json");
            });
        }

        String key = UUID.randomUUID().toString().replace("-", "");
        Instant start = Instant.parse("2026-10-27T14:00:00Z");
        kafka.send(Topics.ALERTS, "T-1", Json.write(new AlertEvent(key, "CANCEL_RATIO", "T-1", null, 60,
                start, start.plusSeconds(60), Map.of("cancels", 40))));
        await().atMost(Duration.ofSeconds(30)).until(() -> alerts.findByAlertKey(key).isPresent());
    }
}