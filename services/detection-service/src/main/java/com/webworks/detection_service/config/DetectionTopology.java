package com.webworks.detection_service.config;

import com.webworks.detection_service.rules.CancelRatioRule;
import com.webworks.detection_service.rules.SpoofingRule;
import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.JsonSerde;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class DetectionTopology {

    @Bean
    public KStream<String, OrderEvent> orderStream(StreamsBuilder builder) {
        KStream<String, OrderEvent> orders = builder.stream(Topics.ORDERS,
                Consumed.with(Serdes.String(), new JsonSerde<>(OrderEvent.class)));
        Produced<String, AlertEvent> alertsOut = Produced.with(Serdes.String(), new JsonSerde<>(AlertEvent.class));

        CancelRatioRule.apply(orders).to(Topics.ALERTS, alertsOut);
        SpoofingRule.apply(orders).to(Topics.ALERTS, alertsOut);

        return orders;
    }
}