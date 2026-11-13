package com.webworks.ingestion_service.service;

import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final Counter rejected;

    public OrderPublisher(KafkaTemplate<String, String> kafka, MeterRegistry registry) {
        this.kafka = kafka;
        this.rejected = Counter.builder("orders.rejected")
                .description("Orders that failed validation")
                .register(registry);
    }

    /** Returns the validation problems; an empty list means the order was published. */
    public List<String> publish(OrderEvent e) {
        List<String> errors = OrderValidator.validate(e);
        if (!errors.isEmpty()) {
            rejected.increment();
            kafka.send(Topics.ORDERS_INVALID, e == null ? null : e.traderId(), Json.write(e));
            return errors;
        }
        kafka.send(Topics.ORDERS, e.traderId(), Json.write(e));
        return List.of();
    }

    public long rejectedCount() {
        return (long) rejected.count();
    }
}