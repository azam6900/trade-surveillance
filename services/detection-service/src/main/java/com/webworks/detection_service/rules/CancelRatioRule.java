package com.webworks.detection_service.rules;

import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.JsonSerde;
import com.webworks.tradecommon.OrderEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;

public final class CancelRatioRule {

    static final int MIN_CANCELS = 20;
    static final double RATIO = 10.0;

    private CancelRatioRule() {
    }

    public static KStream<String, AlertEvent> apply(KStream<String, OrderEvent> orders) {
        Serde<OrderEvent> orderSerde = new JsonSerde<>(OrderEvent.class);
        Serde<OtrState> stateSerde = new JsonSerde<>(OtrState.class);

        return orders
                .groupByKey(Grouped.with(Serdes.String(), orderSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .aggregate(
                        OtrState::new,
                        (traderId, event, state) -> state.apply(event, MIN_CANCELS, RATIO),
                        Materialized.<String, OtrState, WindowStore<Bytes, byte[]>>as("otr-state-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(stateSerde)
                                .withCachingDisabled())
                .toStream()
                .filter((windowedKey, state) -> state != null && state.emit)
                .map((windowedKey, state) -> KeyValue.pair(windowedKey.key(), AlertFactory.cancelRatio(windowedKey, state)));
    }
}