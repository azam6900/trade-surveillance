package com.webworks.detection_service.rules;

import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.JsonSerde;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.OrderEvent.Action;
import com.webworks.tradecommon.OrderEvent.Side;
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
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SpoofingRuleTest {

    private static final Instant T0 = Instant.parse("2026-10-27T14:00:00.200Z");
    private final Serde<OrderEvent> orderSerde = new JsonSerde<>(OrderEvent.class);
    private final Serde<AlertEvent> alertSerde = new JsonSerde<>(AlertEvent.class);

    private TopologyTestDriver driver;
    private TestInputTopic<String, OrderEvent> input;
    private TestOutputTopic<String, AlertEvent> output;

    @BeforeEach
    void setUp() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, OrderEvent> orders = builder.stream(Topics.ORDERS, Consumed.with(Serdes.String(), orderSerde));
        SpoofingRule.apply(orders).to(Topics.ALERTS, Produced.with(Serdes.String(), alertSerde));

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "spoofing-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        driver = new TopologyTestDriver(builder.build(), props);
        input = driver.createInputTopic(Topics.ORDERS, Serdes.String().serializer(), orderSerde.serializer());
        output = driver.createOutputTopic(Topics.ALERTS, Serdes.String().deserializer(), alertSerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    private void send(String id, Action action, Side side, long qty, long offsetMs) {
        Instant at = T0.plusMillis(offsetMs);
        var e = new OrderEvent(id, "T-77", "RY.TO", side, OrderEvent.OrderType.LIMIT,
                new BigDecimal("140.00"), qty, action, at);
        input.pipeInput("T-77", e, at);
    }

    private void sendSequence(boolean withFill) {
        for (int i = 1; i <= 5; i++) send("L-" + i, Action.NEW, Side.BUY, 10_000, 200L * i);
        send("S-1", Action.NEW, Side.SELL, 500, 1500);
        if (withFill) send("S-1", Action.FILL, Side.SELL, 500, 1700);
        for (int i = 1; i <= 5; i++) send("L-" + i, Action.CANCEL, Side.BUY, 10_000, 3000 + 200L * i);
    }

    @Test
    @SuppressWarnings("unchecked")
    void spoofingSequenceProducesOneAlertWithEvidence() {
        sendSequence(true);

        List<AlertEvent> alerts = output.readValuesToList();
        assertThat(alerts).hasSize(1);
        AlertEvent alert = alerts.get(0);
        assertThat(alert.rule()).isEqualTo("SPOOFING");
        assertThat(alert.symbol()).isEqualTo("RY.TO");
        List<Map<String, Object>> events = (List<Map<String, Object>>) alert.evidence().get("events");
        assertThat(events).extracting(m -> m.get("orderId")).contains("L-1", "L-5", "S-1");
    }

    @Test
    void noOppositeFillMeansNoAlert() {
        sendSequence(false);
        assertThat(output.isEmpty()).isTrue();
    }
}