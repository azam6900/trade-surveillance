package com.webworks.detection_service.rules;

import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.JsonSerde;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class CancelRatioRuleTest {

    private final Serde<OrderEvent> orderSerde = new JsonSerde<>(OrderEvent.class);
    private final Serde<AlertEvent> alertSerde = new JsonSerde<>(AlertEvent.class);

    private TopologyTestDriver driver;
    private TestInputTopic<String, OrderEvent> input;
    private TestOutputTopic<String, AlertEvent> output;

    @BeforeEach
    void setUp() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, OrderEvent> orders = builder.stream(Topics.ORDERS, Consumed.with(Serdes.String(), orderSerde));
        CancelRatioRule.apply(orders).to(Topics.ALERTS, Produced.with(Serdes.String(), alertSerde));

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "cancel-ratio-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        driver = new TopologyTestDriver(builder.build(), props);
        input = driver.createInputTopic(Topics.ORDERS, Serdes.String().serializer(), orderSerde.serializer());
        output = driver.createOutputTopic(Topics.ALERTS, Serdes.String().deserializer(), alertSerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    private void send(String trader, OrderEvent.Action action, Instant at) {
        var event = new OrderEvent("O-" + at.toEpochMilli(), trader, "RY.TO", OrderEvent.Side.BUY,
                OrderEvent.OrderType.LIMIT, new BigDecimal("100.00"), 100, action, at);
        input.pipeInput(trader, event, at);
    }

    @Test
    void firesOnceWhenCancelsDominate() {
        Instant t0 = Instant.parse("2026-10-27T14:00:05Z");
        send("T-1", OrderEvent.Action.FILL, t0);
        for (int i = 1; i <= 25; i++) {
            send("T-1", OrderEvent.Action.CANCEL, t0.plusMillis(100L * i));
        }
        List<AlertEvent> alerts = output.readValuesToList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).rule()).isEqualTo("CANCEL_RATIO");
        assertThat(alerts.get(0).traderId()).isEqualTo("T-1");
    }

    @Test
    void staysQuietForNormalTrading() {
        Instant t0 = Instant.parse("2026-10-27T14:00:05Z");
        for (int i = 0; i < 5; i++) {
            send("T-2", OrderEvent.Action.CANCEL, t0.plusMillis(100L * i));
            send("T-2", OrderEvent.Action.FILL, t0.plusMillis(100L * i + 50));
        }
        assertThat(output.isEmpty()).isTrue();
    }
}