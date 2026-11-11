package com.webworks.ingestion_service.service;

import com.webworks.ingestion_service.service.OrderPublisher;
import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "simulator.events-per-second=0")
@Testcontainers
class OrderPublisherIntegrationTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    OrderPublisher publisher;

    @Test
    void publishedOrderArrivesOnOrdersTopic() {
        var order = new OrderEvent("O-IT-1", "T-IT", "RY.TO", OrderEvent.Side.BUY, OrderEvent.OrderType.LIMIT,
                new BigDecimal("150.00"), 100, OrderEvent.Action.NEW, Instant.now());
        publisher.publish(order);

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(Topics.ORDERS));
            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                assertThat(received).anySatisfy(r -> {
                    assertThat(r.key()).isEqualTo("T-IT");
                    assertThat(Json.read(r.value(), OrderEvent.class).orderId()).isEqualTo("O-IT-1");
                });
            });
        }
    }
}